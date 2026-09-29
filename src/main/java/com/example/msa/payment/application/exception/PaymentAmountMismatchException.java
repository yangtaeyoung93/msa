package com.example.msa.payment.application.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentAmountMismatchException extends RuntimeException {
    public PaymentAmountMismatchException(UUID orderId, BigDecimal orderAmount, BigDecimal paidAmount) {
        super("결제 금액이 주문 금액과 일치하지 않아 결제를 취소했습니다. orderId = " + orderId
                + ", orderAmount = " + orderAmount + ", paidAmount = " + paidAmount);
    }
}
