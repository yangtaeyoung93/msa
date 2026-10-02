package com.example.msa.payment.domain.model;

import com.example.msa.payment.application.exception.UnsupportedPaymentProviderException;

import java.util.Locale;

public enum PaymentProvider {
    TEST,
    TOSS;

    // 요청 문자열을 결제사로 변환한다. 비어있거나 알 수 없는 값은 지원하지 않는 결제사로 처리한다.
    public static PaymentProvider from(String value) {
        if (value == null || value.isBlank()) {
            throw new UnsupportedPaymentProviderException(value);
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new UnsupportedPaymentProviderException(value);
        }
    }
}
