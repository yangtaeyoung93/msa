package com.example.msa.order.presentaion.dto.request;

import java.math.BigDecimal;

public record OrderPayRequest(
        String modifierId,
        String provider,
        String paymentKey,
        BigDecimal amount
) {
}
