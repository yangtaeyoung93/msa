package com.example.msa.payment.infrastructure.persistence;

import com.example.msa.payment.domain.model.Payment;
import com.example.msa.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Payment save(Payment payment) {
        return paymentJpaRepository.save(payment);
    }

    @Override
    public Optional<Payment> findByPaymentKey(String paymentKey) {
        return paymentJpaRepository.findByPaymentKey(paymentKey);
    }
}
