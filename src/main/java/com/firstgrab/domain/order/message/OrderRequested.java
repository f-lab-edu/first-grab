package com.firstgrab.domain.order.message;

public record OrderRequested(String orderId, Long eventId, Long userId, int quantity) {
}
