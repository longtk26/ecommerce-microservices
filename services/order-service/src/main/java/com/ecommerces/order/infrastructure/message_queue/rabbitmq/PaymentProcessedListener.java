package com.ecommerces.order.infrastructure.message_queue.rabbitmq;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.ecommerces.events.EventRoutes;
import com.ecommerces.events.OrderCompletedEvent;
import com.ecommerces.events.PaymentProcessedEvent;
import com.ecommerces.order.domain.Order;
import com.ecommerces.order.domain.OrderStatus;
import com.ecommerces.order.repository.OrderRepository;

@Component
public class PaymentProcessedListener {
    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessedListener.class);

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    public PaymentProcessedListener(OrderRepository orderRepository, RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_PROCESSED)
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        logger.info("Received payment.processed for orderId={}", event.orderId());

        Order order = orderRepository.findById(UUID.fromString(event.orderId()))
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.orderId()));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            logger.warn("Order {} already COMPLETED, skipping", event.orderId());
            return; // idempotent
        }

        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);

        rabbitTemplate.convertAndSend(EventRoutes.EXCHANGE, EventRoutes.ORDER_COMPLETED,
                new OrderCompletedEvent(event.orderId(), order.getUserId(), order.getTotalAmount()));
    }
}