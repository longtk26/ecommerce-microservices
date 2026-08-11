package com.ecommerces.payment.infrastructure.persistence;

import com.ecommerces.payment.domain.PaymentAttempt;
import com.ecommerces.payment.ports.IPaymentRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentRepositoryAdapter implements IPaymentRepository {

    private final PaymentAttemptJpaRepository jpaRepository;

    public PaymentRepositoryAdapter(PaymentAttemptJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PaymentAttempt save(PaymentAttempt paymentAttempt) {
        return jpaRepository.save(paymentAttempt);
    }

    @Override
    public Optional<PaymentAttempt> findByOrderId(UUID orderId) {
        return jpaRepository.findByOrderId(orderId);
    }
}
