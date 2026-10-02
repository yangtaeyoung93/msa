package com.example.msa.payment.infrastructure.adapter.toss;

import com.example.msa.payment.domain.gateway.PaymentApproveCommand;
import com.example.msa.payment.domain.gateway.PaymentCancelCommand;
import com.example.msa.payment.domain.gateway.PaymentGateway;
import com.example.msa.payment.domain.model.PaymentProvider;
import com.example.msa.payment.domain.model.PaymentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RequiredArgsConstructor
public class TossPaymentAdapter implements PaymentGateway {

    private final RestClient restClient;
    private final TossPaymentTranslator translator;

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.TOSS;
    }


    @Override
    public PaymentResult approve(PaymentApproveCommand command) {          // throws 제거
        log.info("TOSS 결제 승인 요청: paymentKey={}, orderId={}, amount={}",
                command.paymentKey(), command.orderId(), command.amount());

        TossConfirmRequest body = translator.toConfirmRequest(command);

        return restClient.post()                                            // ← 결과를 반환
                .uri("/v1/payments/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", command.paymentKey())
                .body(body)
                .exchange((req, res) -> {
                    if (res.getStatusCode().is2xxSuccessful()) {
                        return translator.toResult(res.bodyTo(TossPaymentResponse.class));
                    }
                    TossErrorResponse error = res.bodyTo(TossErrorResponse.class);
                    log.warn("TOSS 결제 승인 실패: paymentKey={}, status={}, code={}",
                            command.paymentKey(), res.getStatusCode(),
                            error != null ? error.code() : null);
                    return translator.toFailure(command, error);
                });
    }

    @Override
    public PaymentResult cancel(PaymentCancelCommand command) {
        return null;
    }
}
