package com.example.msa.payment.domain.gateway;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCancelCommand(String paymentKey, UUID orderId, BigDecimal amount, String reason) {
}
