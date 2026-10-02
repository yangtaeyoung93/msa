package com.example.msa.payment.infrastructure.adapter;

import com.example.msa.payment.application.exception.UnsupportedPaymentProviderException;
import com.example.msa.payment.domain.gateway.PaymentGateway;
import com.example.msa.payment.domain.model.PaymentProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentAdapterRegistry {

    private final Map<PaymentProvider, PaymentGateway> gateways = new EnumMap<>(PaymentProvider.class);

    public PaymentAdapterRegistry(List<PaymentGateway> gatewayList) {
        for (PaymentGateway gateway : gatewayList) {
            if (gateways.put(gateway.provider(), gateway) != null) {
                throw new IllegalStateException("결제사 게이트웨이가 중복 등록되었습니다. provider = " + gateway.provider());
            }
        }
    }

    public PaymentGateway get(PaymentProvider provider) {
        PaymentGateway gateway = gateways.get(provider);
        if (gateway == null) {
            throw new UnsupportedPaymentProviderException(String.valueOf(provider));
        }
        return gateway;
    }
}
