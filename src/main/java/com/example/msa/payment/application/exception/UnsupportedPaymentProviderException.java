package com.example.msa.payment.application.exception;

public class UnsupportedPaymentProviderException extends RuntimeException {
    public UnsupportedPaymentProviderException(String provider) {
        super("지원하지 않는 결제사입니다. provider = " + provider);
    }
}
