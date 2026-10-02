package com.example.msa.payment.domain.gateway;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentApproveCommand(String paymentKey,UUID orderId, BigDecimal amount) {
}
