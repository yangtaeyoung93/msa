package com.example.msa.payment.infrastructure.adapter.toss;

import java.math.BigDecimal;

public record TossConfirmRequest(String paymentKey, String orderId , BigDecimal amount) {
}
