package com.example.msa.payment.infrastructure.gateway.testpayment;

import com.example.msa.payment.domain.model.PaymentFailureReason;
import com.example.msa.payment.domain.model.PaymentProvider;
import com.example.msa.payment.domain.model.PaymentResult;
import com.example.msa.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TestPaymentTranslatorTest {

    private final TestPaymentTranslator translator = new TestPaymentTranslator();
    private final UUID orderId = UUID.randomUUID();

    private TestPaymentResponse response(String state, String errorCode, String errorMessage) {
        return new TestPaymentResponse("pay-1", orderId.toString(), state, new BigDecimal("2000"),
                "CREDIT_CARD", "2026-09-29T10:15:30+09:00", errorCode, errorMessage);
    }

    @Test
    @DisplayName("승인 응답(APPROVED)을 공통 모델로 번역한다: 금액 scale 2, CREDIT_CARD→CARD, 시각 통일")
    void translate_approved() {
        PaymentResult result = translator.translate(response("APPROVED", null, null));

        assertThat(result.provider()).isEqualTo(PaymentProvider.TEST);
        assertThat(result.paymentKey()).isEqualTo("pay-1");
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.amount()).isEqualTo(new BigDecimal("2000.00"));
        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.method()).isEqualTo("CARD");
        assertThat(result.approvedAt()).isEqualTo(OffsetDateTime.parse("2026-09-29T10:15:30+09:00")
                .atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
        assertThat(result.failureReason()).isNull();
        assertThat(result.failureMessage()).isNull();
    }

    @Test
    @DisplayName("카드 거절(REJECTED + CARD_REFUSED)은 FAILED + CARD_DECLINED로 번역하고 원본 메시지를 보존한다")
    void translate_rejected() {
        PaymentResult result = translator.translate(
                response("REJECTED", "CARD_REFUSED", "카드사에서 승인을 거절했습니다."));

        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo(PaymentFailureReason.CARD_DECLINED);
        assertThat(result.failureMessage()).isEqualTo("카드사에서 승인을 거절했습니다.");
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "CARD_REFUSED,CARD_DECLINED",
            "NO_BALANCE,INSUFFICIENT_BALANCE",
            "BAD_REQUEST,INVALID_REQUEST",
            "SOMETHING_NEW,UNKNOWN"
    })
    @DisplayName("에러 코드를 공통 실패 사유로 번역한다(매핑에 없는 값은 UNKNOWN)")
    void translate_errorCodeMapping(String errorCode, PaymentFailureReason expected) {
        PaymentResult result = translator.translate(response("REJECTED", errorCode, "오류"));

        assertThat(result.failureReason()).isEqualTo(expected);
    }

    @Test
    @DisplayName("에러 코드 없이 REJECTED인 응답은 FAILED + UNKNOWN으로 번역한다")
    void translate_rejectedWithoutErrorCode() {
        PaymentResult result = translator.translate(response("REJECTED", null, null));

        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo(PaymentFailureReason.UNKNOWN);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "APPROVED,APPROVED",
            "CANCELED,CANCELED",
            "REJECTED,FAILED",
            "WAITING,PENDING",
            "SOMETHING_NEW,PENDING"
    })
    @DisplayName("상태값을 공통 상태로 번역한다(매핑에 없는 값은 PENDING)")
    void translate_statusMapping(String state, PaymentStatus expected) {
        assertThat(translator.translate(response(state, null, null)).status()).isEqualTo(expected);
    }

    @Test
    @DisplayName("상태값이 없으면 승인 처리하지 않고 PENDING으로 번역한다")
    void translate_nullState() {
        assertThat(translator.translate(response(null, null, null)).status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("승인 시각과 결제 수단이 없어도 번역할 수 있다")
    void translate_withoutApprovedAtAndMethod() {
        TestPaymentResponse response = new TestPaymentResponse("pay-1", orderId.toString(), "CANCELED",
                new BigDecimal("2000"), null, null, null, null);

        PaymentResult result = translator.translate(response);

        assertThat(result.approvedAt()).isNull();
        assertThat(result.method()).isNull();
    }
}
