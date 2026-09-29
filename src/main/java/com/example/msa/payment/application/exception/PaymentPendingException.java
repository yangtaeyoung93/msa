package com.example.msa.payment.application.exception;

import java.util.UUID;

public class PaymentPendingException extends RuntimeException {
    public PaymentPendingException(UUID orderId) {
        super("결제 결과를 확인 중입니다. 잠시 후 다시 확인해주세요. orderId = " + orderId);
    }
}
