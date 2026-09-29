package com.example.msa.payment.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResult(
        PaymentProvider provider,
        String paymentKey,
        UUID orderId,
        BigDecimal amount,
        PaymentStatus status,
        String method,
        LocalDateTime approvedAt,
        PaymentFailureReason failureReason,
        String failureMessage
) {
}
