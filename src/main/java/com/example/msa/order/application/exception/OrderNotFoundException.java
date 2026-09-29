package com.example.msa.order.application.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(UUID orderId) {
        super("주문을 찾을 수 없습니다. orderId = " + orderId);
    }
}
