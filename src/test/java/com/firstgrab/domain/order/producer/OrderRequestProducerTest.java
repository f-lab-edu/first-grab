package com.firstgrab.domain.order.producer;

import com.firstgrab.domain.order.message.OrderRequested;
import com.firstgrab.global.exception.OrderPublishException;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaProducerException;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static com.firstgrab.global.config.KafkaTopicConfig.ORDER_REQUESTS_TOPIC;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRequestProducerTest {

    private static final String ORDER_ID = "order-1";
    private static final OrderRequested MESSAGE = new OrderRequested(ORDER_ID, 1L, 101L, 2);

    @Mock
    private KafkaTemplate<String, OrderRequested> kafkaTemplate;

    @InjectMocks
    private OrderRequestProducer orderRequestProducer;

    @Test
    @DisplayName("주문 요청 발행 성공")
    void sendSuccess() {
        when(kafkaTemplate.send(ORDER_REQUESTS_TOPIC, ORDER_ID, MESSAGE))
                .thenReturn(CompletableFuture.completedFuture(null));

        assertThatCode(() -> orderRequestProducer.send(MESSAGE))
                .doesNotThrowAnyException();
        verify(kafkaTemplate).send(ORDER_REQUESTS_TOPIC, ORDER_ID, MESSAGE);
    }

    @Test
    @DisplayName("브로커 응답 실패 시 발행 예외")
    void sendBrokerFailure() {
        KafkaProducerException cause = new KafkaProducerException(
                null, "Failed to send", new RuntimeException("broker"));
        when(kafkaTemplate.send(ORDER_REQUESTS_TOPIC, ORDER_ID, MESSAGE))
                .thenReturn(CompletableFuture.failedFuture(cause));

        assertThatThrownBy(() -> orderRequestProducer.send(MESSAGE))
                .isInstanceOf(OrderPublishException.class)
                .hasCause(cause);
    }

    @Test
    @DisplayName("전송 즉시 실패 시 발행 예외")
    void sendSpringKafkaException() {
        when(kafkaTemplate.send(ORDER_REQUESTS_TOPIC, ORDER_ID, MESSAGE))
                .thenThrow(new KafkaException("Send failed"));

        assertThatThrownBy(() -> orderRequestProducer.send(MESSAGE))
                .isInstanceOf(OrderPublishException.class)
                .hasCauseInstanceOf(KafkaException.class);
    }

    @Test
    @DisplayName("브로커 연결 실패 시 발행 예외")
    void sendClientKafkaException() {
        when(kafkaTemplate.send(ORDER_REQUESTS_TOPIC, ORDER_ID, MESSAGE))
                .thenThrow(new org.apache.kafka.common.errors.TimeoutException("metadata"));

        assertThatThrownBy(() -> orderRequestProducer.send(MESSAGE))
                .isInstanceOf(OrderPublishException.class)
                .hasCauseInstanceOf(org.apache.kafka.common.errors.TimeoutException.class);
    }

    @Test
    @DisplayName("직렬화 실패 시 예외 그대로 전파")
    void sendSerializationFailure() {
        when(kafkaTemplate.send(ORDER_REQUESTS_TOPIC, ORDER_ID, MESSAGE))
                .thenThrow(new SerializationException("invalid"));

        assertThatThrownBy(() -> orderRequestProducer.send(MESSAGE))
                .isInstanceOf(SerializationException.class);
    }

    @Test
    @DisplayName("응답 대기 시간 초과 시 발행 예외")
    void sendTimeout() {
        when(kafkaTemplate.send(ORDER_REQUESTS_TOPIC, ORDER_ID, MESSAGE))
                .thenReturn(new CompletableFuture<>());

        assertThatThrownBy(() -> orderRequestProducer.send(MESSAGE))
                .isInstanceOf(OrderPublishException.class)
                .hasCauseInstanceOf(TimeoutException.class);
    }
}