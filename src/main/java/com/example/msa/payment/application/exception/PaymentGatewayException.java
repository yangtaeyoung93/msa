package com.example.msa.payment.application.exception;

import com.example.msa.payment.domain.model.PaymentProvider;
import lombok.Getter;

// 결제사 통신 장애(타임아웃/5xx). 결제 거절과 구분하기 위한 예외이며, 재시도 가능 여부를 함께 전달한다.
@Getter
public class PaymentGatewayException extends RuntimeException {

    private final PaymentProvider provider;
    private final boolean retryable;

    public PaymentGatewayException(PaymentProvider provider, String message, boolean retryable, Throwable cause) {
        super("결제사 통신 중 오류가 발생했습니다. provider = " + provider + ", message = " + message, cause);
        this.provider = provider;
        this.retryable = retryable;
    }
}
