package com.example.msa.order.domain.repository;

import com.example.msa.order.domain.model.Order;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);

    // 주문 항목까지 함께 조회한다.
    Optional<Order> findById(UUID orderId);
}
