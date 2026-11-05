package com.firstgrab.domain.order.consumer;

import com.firstgrab.domain.order.message.OrderRequested;
import com.firstgrab.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.firstgrab.global.config.KafkaTopicConfig.ORDER_REQUESTS_TOPIC;

@RequiredArgsConstructor
@Component
public class OrderRequestConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = ORDER_REQUESTS_TOPIC)
    public void consume(OrderRequested message) {
        orderService.placeOrder(message);
    }
}
