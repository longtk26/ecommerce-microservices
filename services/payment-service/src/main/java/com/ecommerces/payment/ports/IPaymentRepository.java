package com.ecommerces.payment.ports;

import com.ecommerces.payment.domain.PaymentAttempt;

import java.util.Optional;
import java.util.UUID;

public interface IPaymentRepository {

    PaymentAttempt save(PaymentAttempt paymentAttempt);

    Optional<PaymentAttempt> findByOrderId(UUID orderId);
}
