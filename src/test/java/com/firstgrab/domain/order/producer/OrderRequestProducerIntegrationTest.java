package com.firstgrab.domain.order.producer;

import com.firstgrab.domain.order.message.OrderRequested;
import com.firstgrab.global.config.KafkaTopicConfig;
import com.firstgrab.support.KafkaContainerConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.firstgrab.global.config.KafkaTopicConfig.ORDER_REQUESTS_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        ServiceConnectionAutoConfiguration.class,
        KafkaAutoConfiguration.class,
        KafkaTopicConfig.class,
        OrderRequestProducer.class
})
@Import(KafkaContainerConfig.class)
public class OrderRequestProducerIntegrationTest {

    private static final String ORDER_ID = "order-1";
    private static final OrderRequested MESSAGE = new OrderRequested(ORDER_ID, 1L, 10L, 2);

    @Autowired
    private OrderRequestProducer orderRequestProducer;

    @Autowired
    private KafkaConnectionDetails connectionDetails;

    @Test
    @DisplayName("주문 요청을 orderId key와 JSON으로 발행")
    void sendToKafka() {
        orderRequestProducer.send(MESSAGE);

        try (Consumer<String, String> consumer = createConsumer()) {
            consumer.subscribe(List.of(ORDER_REQUESTS_TOPIC));
            ConsumerRecord<String, String> record =
                    KafkaTestUtils.getSingleRecord(consumer, ORDER_REQUESTS_TOPIC, Duration.ofSeconds(10));

            assertThat(record.key()).isEqualTo(ORDER_ID);
            assertThat(record.value())
                    .contains("\"orderId\":\"order-1\"")
                    .contains("\"eventId\":1")
                    .contains("\"quantity\":2");
        }
    }

    private Consumer<String, String> createConsumer() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                String.join(",", connectionDetails.getBootstrapServers()),
                ConsumerConfig.GROUP_ID_CONFIG, "test-group",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(props);
    }
}
