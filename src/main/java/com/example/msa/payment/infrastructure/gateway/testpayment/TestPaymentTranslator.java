package com.example.msa.payment.infrastructure.gateway.testpayment;

import com.example.msa.payment.domain.model.PaymentFailureReason;
import com.example.msa.payment.domain.model.PaymentProvider;
import com.example.msa.payment.domain.model.PaymentResult;
import com.example.msa.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

class TestPaymentTranslator {

    PaymentResult translate(TestPaymentResponse response) {
        PaymentStatus status = toStatus(response.state());
        return new PaymentResult(
                PaymentProvider.TEST,
                response.paymentKey(),
                toOrderId(response.orderId()),
                toAmount(response.amount()),
                status,
                toMethod(response.method()),
                toApprovedAt(response.approvedAt()),
                status == PaymentStatus.FAILED ? toFailureReason(response.errorCode()) : null,
                status == PaymentStatus.FAILED ? response.errorMessage() : null
        );
    }

    private PaymentStatus toStatus(String state) {
        if (state == null) {
            return PaymentStatus.PENDING;
        }
        return switch (state) {
            case "APPROVED" -> PaymentStatus.APPROVED;
            case "CANCELED" -> PaymentStatus.CANCELED;
            case "REJECTED" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }

    private PaymentFailureReason toFailureReason(String errorCode) {
        if (errorCode == null) {
            return PaymentFailureReason.UNKNOWN;
        }
        return switch (errorCode) {
            case "CARD_REFUSED" -> PaymentFailureReason.CARD_DECLINED;
            case "NO_BALANCE" -> PaymentFailureReason.INSUFFICIENT_BALANCE;
            case "BAD_REQUEST" -> PaymentFailureReason.INVALID_REQUEST;
            default -> PaymentFailureReason.UNKNOWN;
        };
    }

    private String toMethod(String method) {
        if (method == null) {
            return null;
        }
        return "CREDIT_CARD".equals(method) ? "CARD" : method;
    }

    private BigDecimal toAmount(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private UUID toOrderId(String orderId) {
        return orderId == null ? null : UUID.fromString(orderId);
    }

    private LocalDateTime toApprovedAt(String approvedAt) {
        if (approvedAt == null) {
            return null;
        }
        return OffsetDateTime.parse(approvedAt).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
}
