package com.example.msa.payment.application.exception;

import com.example.msa.payment.domain.model.PaymentFailureReason;
import lombok.Getter;

@Getter
public class PaymentFailedException extends RuntimeException {

    private final PaymentFailureReason reason;

    public PaymentFailedException(PaymentFailureReason reason, String message) {
        super("결제가 승인되지 않았습니다. reason = " + reason + ", message = " + message);
        this.reason = reason;
    }
}
