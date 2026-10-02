package com.example.msa.payment.infrastructure.adapter.toss;

import com.example.msa.payment.domain.gateway.PaymentApproveCommand;
import com.example.msa.payment.domain.model.PaymentFailureReason;
import com.example.msa.payment.domain.model.PaymentProvider;
import com.example.msa.payment.domain.model.PaymentResult;
import com.example.msa.payment.domain.model.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Component
public class TossPaymentTranslator {


    public TossConfirmRequest toConfirmRequest(PaymentApproveCommand command){
        return new TossConfirmRequest(
                command.paymentKey(),
                command.orderId().toString(),
                command.amount()
        );
    }


    public PaymentResult toResult(TossPaymentResponse response) {
        PaymentStatus status = toStatus(response.status());
        return new PaymentResult(
                PaymentProvider.TOSS,
                response.paymentKey(),
                toOrderId(response.orderId()),
                toAmount(response.totalAmount()),
                status,
                toMethod(response.method()),
                toApprovedAt(response.approvedAt()),
                status == PaymentStatus.FAILED ? toFailureReason(response.error().code()) : null,
                status == PaymentStatus.FAILED ? response.error().message() : null
        );
    }

    public PaymentResult toFailure(PaymentApproveCommand command, TossErrorResponse error) {
        return new PaymentResult(
                PaymentProvider.TOSS,
                command.paymentKey(),
                command.orderId(),     // 에러 객체에는 주문 정보가 없으므로 우리 요청값 사용
                command.amount(),
                PaymentStatus.FAILED,
                null,
                null,
                toFailureReason(error.code()),
                error.message()
        );
    }


    private PaymentStatus toStatus(String state) {
        if (state == null) {
            return PaymentStatus.PENDING;
        }
        return switch (state) {
            case "DONE" -> PaymentStatus.APPROVED;
            case "CANCELED" -> PaymentStatus.CANCELED;
            case "ABORTED" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }

    private PaymentFailureReason toFailureReason(String errorCode) {
        if (errorCode == null) {
            return PaymentFailureReason.UNKNOWN;
        }
        return switch (errorCode) {
            case "CARD_PROCESSING_ERROR" -> PaymentFailureReason.CARD_DECLINED;
            case "UNAPPROVED_ORDER_ID" -> PaymentFailureReason.INSUFFICIENT_BALANCE;
            case "INVALID_REQUEST" -> PaymentFailureReason.INVALID_REQUEST;
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
