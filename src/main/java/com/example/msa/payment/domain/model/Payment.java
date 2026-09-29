package com.example.msa.payment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name = "payment", schema = "public")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProvider provider;

    @Column(name = "payment_key", nullable = false, unique = true, length = 200)
    private String paymentKey;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 50)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 30)
    private PaymentFailureReason failureReason;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "reg_id", nullable = false)
    private UUID regId;

    @Column(name = "reg_dt", nullable = false)
    private LocalDateTime regDt;

    @Column(name = "modify_id", nullable = false)
    private UUID modifyId;

    @Column(name = "modify_dt", nullable = false)
    private LocalDateTime modifyDt;

    public static Payment from(PaymentResult result, UUID actorId) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.orderId = result.orderId();
        payment.provider = result.provider();
        payment.paymentKey = result.paymentKey();
        payment.regId = actorId;
        payment.regDt = LocalDateTime.now();
        payment.apply(result, actorId);
        return payment;
    }

    // 같은 paymentKey로 재시도된 결제의 최신 결과를 반영한다.
    public void applyResult(PaymentResult result, UUID actorId) {
        apply(result, actorId);
    }

    private void apply(PaymentResult result, UUID actorId) {
        this.amount = result.amount();
        this.status = result.status();
        this.method = result.method();
        this.failureReason = result.failureReason();
        this.failureMessage = result.failureMessage();
        this.approvedAt = result.approvedAt();
        this.modifyId = actorId;
        this.modifyDt = LocalDateTime.now();
    }
}
