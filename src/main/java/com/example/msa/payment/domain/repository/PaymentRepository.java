package com.example.msa.payment.domain.repository;

import com.example.msa.payment.domain.model.Payment;

import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);

    Optional<Payment> findByPaymentKey(String paymentKey);
}
