package com.firstgrab.domain.order.producer;

import com.firstgrab.domain.order.message.OrderRequested;
import com.firstgrab.global.exception.OrderPublishException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.firstgrab.global.config.KafkaTopicConfig.ORDER_REQUESTS_TOPIC;

@RequiredArgsConstructor
@Component
public class OrderRequestProducer {

    private static final long SEND_TIMEOUT_SECONDS = 6;

    private final KafkaTemplate<String, OrderRequested> kafkaTemplate;

    public void send(OrderRequested message) {
        try {
            kafkaTemplate.send(ORDER_REQUESTS_TOPIC, message.orderId(), message)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderPublishException(message.orderId(), e);
        } catch (ExecutionException e) {
            throw new OrderPublishException(message.orderId(), e.getCause());
        } catch (SerializationException e) {
            throw e;
        } catch (TimeoutException
                 | KafkaException
                 | org.apache.kafka.common.KafkaException e) {
            throw new OrderPublishException(message.orderId(), e);
        }
    }
}
