package com.example.msa.payment.domain.model;

// 결제사별 에러 코드를 번역한 공통 실패 사유
public enum PaymentFailureReason {
    CARD_DECLINED,
    INSUFFICIENT_BALANCE,
    INVALID_REQUEST,
    EXPIRED,
    UNKNOWN
}
