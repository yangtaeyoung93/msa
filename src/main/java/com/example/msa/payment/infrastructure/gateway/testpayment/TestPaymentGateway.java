package com.example.msa.payment.infrastructure.gateway.testpayment;

import com.example.msa.payment.domain.gateway.PaymentApproveCommand;
import com.example.msa.payment.domain.gateway.PaymentCancelCommand;
import com.example.msa.payment.domain.gateway.PaymentGateway;
import com.example.msa.payment.domain.model.PaymentProvider;
import com.example.msa.payment.domain.model.PaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Slf4j
@Component
public class TestPaymentGateway implements PaymentGateway {

    private static final String REJECT_PREFIX = "fail-";

    private final TestPaymentTranslator translator = new TestPaymentTranslator();

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.TEST;
    }

    @Override
    public PaymentResult approve(PaymentApproveCommand command) {
        log.info("[TEST-PAYMENT] 결제 승인 요청: paymentKey={}, orderId={}, amount={}",
                command.paymentKey(), command.orderId(), command.amount());

        TestPaymentResponse response = command.paymentKey().startsWith(REJECT_PREFIX)
                ? new TestPaymentResponse(command.paymentKey(), command.orderId().toString(), "REJECTED",
                command.amount(), null, null, "CARD_REFUSED", "카드사에서 승인을 거절했습니다.")
                : new TestPaymentResponse(command.paymentKey(), command.orderId().toString(), "APPROVED",
                command.amount(), "CREDIT_CARD", OffsetDateTime.now().toString(), null, null);

        log.info("[TEST-PAYMENT] 결제 승인 응답: state={}, errorCode={}", response.state(), response.errorCode());
        return translator.translate(response);
    }

    @Override
    public PaymentResult cancel(PaymentCancelCommand command) {
        log.info("[TEST-PAYMENT] 결제 취소 요청: paymentKey={}, orderId={}, amount={}, reason={}",
                command.paymentKey(), command.orderId(), command.amount(), command.reason());

        TestPaymentResponse response = new TestPaymentResponse(command.paymentKey(), command.orderId().toString(),
                "CANCELED", command.amount(), "CREDIT_CARD", null, null, null);
        return translator.translate(response);
    }
}
