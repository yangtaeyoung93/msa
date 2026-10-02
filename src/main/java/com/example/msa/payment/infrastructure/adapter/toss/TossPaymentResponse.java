package com.example.msa.payment.infrastructure.adapter.toss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentResponse(
    String paymentKey,
    String type,
    String orderId,
    String orderName,
    String mid,
    String currency,
    @Nullable
    String method,
    BigDecimal totalAmount,
    BigDecimal balanceAmount,
    String status,
    String requestedAt,
    String approvedAt,
    Error error

) {

    public record Error(String code, String message){}
}
