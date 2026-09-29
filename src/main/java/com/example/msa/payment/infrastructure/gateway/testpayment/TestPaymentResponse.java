package com.example.msa.payment.infrastructure.gateway.testpayment;

import java.math.BigDecimal;

record TestPaymentResponse(
        String paymentKey,
        String orderId,
        String state,
        BigDecimal amount,
        String method,
        String approvedAt,
        String errorCode,
        String errorMessage
) {
}
