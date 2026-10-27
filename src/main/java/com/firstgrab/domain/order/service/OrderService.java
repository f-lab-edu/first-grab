package com.firstgrab.domain.order.service;

import com.firstgrab.domain.event.repository.EventRepository;
import com.firstgrab.domain.order.entity.Order;
import com.firstgrab.domain.order.message.OrderRequested;
import com.firstgrab.domain.order.repository.OrderRepository;
import com.firstgrab.global.exception.InvalidOrderMessageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;

    @Transactional
    public OrderResult placeOrder(OrderRequested message) {
        validateQuantity(message.quantity(), message.orderId());

        if (orderRepository.existsByOrderId(message.orderId())) {
            log.info("Duplicate order skipped, orderId={}", message.orderId());
            return OrderResult.DUPLICATE;
        }

        boolean reserved = eventRepository.increaseSoldIfAvailable(
                message.eventId(), message.quantity()) > 0;

        if (!reserved) {
            orderRepository.save(Order.failed(
                    message.orderId(), message.eventId(),
                    message.userId(), message.quantity()));
            log.warn("Order failed, insufficient stock, orderId={}, eventId={}", message.orderId(), message.eventId());
            return OrderResult.FAILED;
        }

        orderRepository.save(Order.pending(
                message.orderId(), message.eventId(),
                message.userId(), message.quantity()));
        return OrderResult.PENDING;
    }

    private void validateQuantity(int quantity, String orderId) {
        if (quantity <= 0) {
            throw new InvalidOrderMessageException(orderId, quantity);
        }
    }
}
