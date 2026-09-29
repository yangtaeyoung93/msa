package com.example.msa.order.application.exception;

import com.example.msa.order.domain.model.OrderStatus;

import java.util.UUID;

public class OrderNotPayableException extends RuntimeException {
    public OrderNotPayableException(UUID orderId, OrderStatus status) {
        super("결제할 수 없는 주문 상태입니다. orderId = " + orderId + ", status = " + status);
    }
}
