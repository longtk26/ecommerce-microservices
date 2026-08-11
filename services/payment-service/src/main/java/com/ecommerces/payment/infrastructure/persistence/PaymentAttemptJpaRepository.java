package com.ecommerces.payment.infrastructure.persistence;

import com.ecommerces.payment.domain.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface PaymentAttemptJpaRepository extends JpaRepository<PaymentAttempt, UUID> {

    Optional<PaymentAttempt> findByOrderId(UUID orderId);
}
