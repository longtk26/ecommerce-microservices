package com.ecommerces.payment.usecases;

import com.ecommerces.events.EventRoutes;
import com.ecommerces.events.PaymentProcessedEvent;
import com.ecommerces.events.PaymentProcessedEvent.OrderItemPayload;
import com.ecommerces.payment.domain.PaymentAttempt;
import com.ecommerces.payment.domain.PaymentStatus;
import com.ecommerces.payment.infrastructure.http.dto.OrderDetailsDto;
import com.ecommerces.payment.ports.IMessageQueue;
import com.ecommerces.payment.ports.IOrderServiceClient;
import com.ecommerces.payment.presentation.dto.ProcessPaymentRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ecommerces.payment.ports.IPaymentRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ProcessPaymentUseCase {

    private final IPaymentRepository paymentRepository;
    private final IMessageQueue messageQueue;
    private final IOrderServiceClient orderServiceClient;

    public ProcessPaymentUseCase(IPaymentRepository paymentRepository,
            IMessageQueue messageQueue,
            IOrderServiceClient orderServiceClient) {
        this.paymentRepository = paymentRepository;
        this.messageQueue = messageQueue;
        this.orderServiceClient = orderServiceClient;
    }

    @Transactional
    public void execute(ProcessPaymentRequestDto dto) {
        // 1. Fetch order details from order-service — never trust the frontend for
        // amount/userId.
        OrderDetailsDto order = orderServiceClient.getOrderById(dto.getOrderId());

        // 2. Simulate payment processing (always succeeds for now — real gateway in
        // Epic 3).
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setOrderId(UUID.fromString(order.orderId()));
        attempt.setAmount(order.totalAmount());
        attempt.setStatus(PaymentStatus.SUCCESS);
        attempt.setTransactionId("txn-" + UUID.randomUUID());

        paymentRepository.save(attempt);

        // 3. Build the event payload from the authoritative order data.
        List<OrderItemPayload> itemPayloads = order.items().stream()
                .map(i -> new OrderItemPayload(
                        i.productId(),
                        i.productName(),
                        i.unitPrice(),
                        i.quantity()))
                .toList();

        PaymentProcessedEvent event = new PaymentProcessedEvent(
                order.orderId(),
                order.userId(),
                order.shopId(),
                itemPayloads,
                order.totalAmount());

        // 4. Publish AFTER the DB transaction commits to guarantee the payment record
        // exists before downstream services react to the event.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messageQueue.publish(EventRoutes.EXCHANGE, EventRoutes.PAYMENT_PROCESSED, event);
            }
        });
    }
}
