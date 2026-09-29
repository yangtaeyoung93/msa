package com.example.msa.payment.domain.gateway;

import com.example.msa.payment.domain.model.PaymentProvider;
import com.example.msa.payment.domain.model.PaymentResult;

public interface PaymentGateway {

    PaymentProvider provider();

    PaymentResult approve(PaymentApproveCommand command);

    PaymentResult cancel(PaymentCancelCommand command);
}
