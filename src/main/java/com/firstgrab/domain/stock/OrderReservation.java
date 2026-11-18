package com.firstgrab.domain.stock;

public record OrderReservation(String orderId, Long eventId, Long userId, int quantity, int perUserLimit) {
}
