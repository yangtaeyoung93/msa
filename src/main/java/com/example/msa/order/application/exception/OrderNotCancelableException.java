package com.example.msa.order.application.exception;

import java.util.UUID;

// 결제 완료(PAID)된 주문은 환불이 범위 밖이므로 취소할 수 없다.
public class OrderNotCancelableException extends RuntimeException {
    public OrderNotCancelableException(UUID orderId) {
        super("결제가 완료된 주문은 취소할 수 없습니다. orderId = " + orderId);
    }
}
