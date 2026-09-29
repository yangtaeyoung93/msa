package com.example.msa.payment.infrastructure.gateway.testpayment;

import com.example.msa.payment.domain.gateway.PaymentGateway;
import com.example.msa.payment.domain.model.PaymentProvider;
import com.example.msa.payment.infrastructure.gateway.PaymentGatewayContractTest;

class TestPaymentGatewayTest extends PaymentGatewayContractTest {

    @Override
    protected PaymentGateway gateway() {
        return new TestPaymentGateway();
    }

    @Override
    protected PaymentProvider expectedProvider() {
        return PaymentProvider.TEST;
    }
}
