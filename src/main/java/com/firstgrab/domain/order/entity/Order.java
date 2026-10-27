package com.firstgrab.domain.order.entity;

import com.firstgrab.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String orderId;

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    private Order(String orderId, Long eventId, Long userId, int quantity, OrderStatus status) {
        this.orderId = orderId;
        this.eventId = eventId;
        this.userId = userId;
        this.quantity = quantity;
        this.status = status;
    }

    public static Order pending(String orderId, Long eventId, Long userId, int quantity) {
        return new Order(orderId, eventId, userId, quantity, OrderStatus.PENDING);
    }

    public static Order failed(String orderId, Long eventId, Long userId, int quantity) {
        return new Order(orderId, eventId, userId, quantity, OrderStatus.FAILED);
    }
}
