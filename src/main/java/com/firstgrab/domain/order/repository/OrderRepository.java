package com.firstgrab.domain.order.repository;

import com.firstgrab.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByOrderId(String orderId);
}
