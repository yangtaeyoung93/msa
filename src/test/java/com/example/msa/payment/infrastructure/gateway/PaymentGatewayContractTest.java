package com.example.msa.payment.infrastructure.gateway;

import com.example.msa.payment.domain.gateway.PaymentApproveCommand;
import com.example.msa.payment.domain.gateway.PaymentCancelCommand;
import com.example.msa.payment.domain.gateway.PaymentGateway;
import com.example.msa.payment.domain.model.PaymentFailureReason;
import com.example.msa.payment.domain.model.PaymentProvider;
import com.example.msa.payment.domain.model.PaymentResult;
import com.example.msa.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 어떤 결제사 어댑터든 같은 입력에 대해 같은 공통 규격의 결과를 돌려줘야 한다(ACL 계약).
// 각 어댑터 테스트가 이 클래스를 상속해 같은 시나리오를 통과시킨다.
public abstract class PaymentGatewayContractTest {

    protected abstract PaymentGateway gateway();

    protected abstract PaymentProvider expectedProvider();

    private final UUID orderId = UUID.randomUUID();

    @Test
    @DisplayName("계약: provider()는 자신의 결제사를 반환한다")
    void provider() {
        assertThat(gateway().provider()).isEqualTo(expectedProvider());
    }

    @Test
    @DisplayName("계약: 승인 요청은 APPROVED와 요청값(paymentKey/orderId/금액 scale 2)을 담은 공통 결과를 돌려준다")
    void approve_success() {
        PaymentResult result = gateway().approve(new PaymentApproveCommand("pay-1", orderId, new BigDecimal("2000")));

        assertThat(result.provider()).isEqualTo(expectedProvider());
        assertThat(result.paymentKey()).isEqualTo("pay-1");
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.amount()).isEqualTo(new BigDecimal("2000.00"));
        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.method()).isEqualTo("CARD");
        assertThat(result.approvedAt()).isNotNull();
        assertThat(result.failureReason()).isNull();
        assertThat(result.failureMessage()).isNull();
    }

    @Test
    @DisplayName("계약: fail- 접두어 paymentKey는 FAILED + CARD_DECLINED와 실패 메시지를 돌려준다(예외를 던지지 않는다)")
    void approve_rejected() {
        PaymentResult result = gateway().approve(new PaymentApproveCommand("fail-1", orderId, new BigDecimal("2000")));

        assertThat(result.provider()).isEqualTo(expectedProvider());
        assertThat(result.paymentKey()).isEqualTo("fail-1");
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo(PaymentFailureReason.CARD_DECLINED);
        assertThat(result.failureMessage()).isNotBlank();
        assertThat(result.approvedAt()).isNull();
    }

    @Test
    @DisplayName("계약: 취소 요청은 CANCELED와 요청값을 담은 공통 결과를 돌려준다")
    void cancel_success() {
        PaymentResult result = gateway().cancel(
                new PaymentCancelCommand("pay-1", orderId, new BigDecimal("2000"), "결제 금액 불일치"));

        assertThat(result.provider()).isEqualTo(expectedProvider());
        assertThat(result.paymentKey()).isEqualTo("pay-1");
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.amount()).isEqualTo(new BigDecimal("2000.00"));
        assertThat(result.status()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(result.failureReason()).isNull();
    }
}
