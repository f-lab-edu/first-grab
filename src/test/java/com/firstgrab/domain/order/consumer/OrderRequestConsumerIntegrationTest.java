package com.firstgrab.domain.order.consumer;

import com.firstgrab.domain.order.message.OrderRequested;
import com.firstgrab.domain.order.service.OrderResult;
import com.firstgrab.domain.order.service.OrderService;
import com.firstgrab.global.config.KafkaConsumerConfig;
import com.firstgrab.global.config.KafkaTopicConfig;
import com.firstgrab.global.exception.InvalidOrderMessageException;
import com.firstgrab.support.KafkaContainerConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.firstgrab.global.config.KafkaTopicConfig.ORDER_REQUESTS_DLT;
import static com.firstgrab.global.config.KafkaTopicConfig.ORDER_REQUESTS_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        ServiceConnectionAutoConfiguration.class,
        KafkaAutoConfiguration.class,
        KafkaTopicConfig.class,
        KafkaConsumerConfig.class,
        OrderRequestConsumer.class,
        OrderDltConsumer.class
})
@Import(KafkaContainerConfig.class)
public class OrderRequestConsumerIntegrationTest {

    @MockitoBean
    private OrderService orderService;

    @Autowired
    private KafkaConnectionDetails connectionDetails;

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    @DisplayName("주문 메시지 처리")
    void consumeSuccess() {
        String orderId = "order-success";
        OrderRequested message = new OrderRequested(orderId, 1L, 10L, 2);
        when(orderService.placeOrder(message)).thenReturn(OrderResult.PENDING);

        send(orderId, toJson(message));

        verify(orderService, timeout(10000)).placeOrder(message);
    }

    @Test
    @DisplayName("잘못된 수량은 재시도 없이 DLT")
    void consumeInvalidQuantity() {
        String orderId = "order-invalid";
        OrderRequested message = new OrderRequested(orderId, 1L, 10L, 0);
        when(orderService.placeOrder(message))
                .thenThrow(new InvalidOrderMessageException(orderId, 0));

        send(orderId, toJson(message));

        ConsumerRecord<String, String> record = findDltRecord(orderId);
        assertThat(exceptionCause(record))
                .contains("InvalidOrderMessageException");
        verify(orderService, times(1)).placeOrder(message);
    }

    @Test
    @DisplayName("깨진 JSON은 DLT")
    void consumeMalformedJson() {
        String orderId = "order-malformed";
        send(orderId, "not-json");

        ConsumerRecord<String, String> record = findDltRecord(orderId);
        assertThat(record.value()).isEqualTo("not-json");
        assertThat(exceptionCause(record))
                .contains("ConversionException");
        verify(orderService, never()).placeOrder(any());
    }

    @Test
    @DisplayName("계속 실패하면 재시도 후 DLT")
    void consumeRetryThenDlt() {
        String orderId = "order-retry";
        OrderRequested message = new OrderRequested(orderId, 1L, 10L, 2);
        when(orderService.placeOrder(message))
                .thenThrow(new RuntimeException("db down"));

        send(orderId, toJson(message));

        ConsumerRecord<String, String> record = findDltRecord(orderId);
        assertThat(exceptionCause(record))
                .contains("RuntimeException");
        verify(orderService, times(4)).placeOrder(message);
    }

    private String toJson(OrderRequested message) {
        return jsonMapper.writeValueAsString(message);
    }

    private void send(String key, String value) {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(ORDER_REQUESTS_TOPIC, key, value));
        }
    }

    private ConsumerRecord<String, String> findDltRecord(String key) {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (Consumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(ORDER_REQUESTS_DLT));
            long deadline = System.currentTimeMillis() + 20_000;
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (key.equals(record.key())) {
                        return record;
                    }
                }
            }
        }
        throw new AssertionError("DLT 메시지를 찾지 못했습니다. key=" + key);
    }

    private String bootstrapServers() {
        return String.join(",", connectionDetails.getBootstrapServers());
    }

    private String exceptionCause(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_CAUSE_FQCN);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
