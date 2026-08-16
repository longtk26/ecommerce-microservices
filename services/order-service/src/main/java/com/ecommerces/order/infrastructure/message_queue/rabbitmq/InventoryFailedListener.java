package com.ecommerces.order.infrastructure.message_queue.rabbitmq;

import com.ecommerces.events.EventRoutes;
import com.ecommerces.events.InventoryFailedEvent;
import com.ecommerces.events.OrderCancelledEvent;
import com.ecommerces.order.domain.Order;
import com.ecommerces.order.domain.OrderStatus;
import com.ecommerces.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Story 3.4 — Listens for {@code inventory.failed} events and cancels the order.
 * Published by inventory-service when stock is insufficient for one or more items.
 */
@Component
public class InventoryFailedListener {

    private static final Logger logger = LoggerFactory.getLogger(InventoryFailedListener.class);

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    public InventoryFailedListener(OrderRepository orderRepository, RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_INVENTORY_FAILED)
    public void handleInventoryFailed(InventoryFailedEvent event) {
        logger.info("Received inventory.failed for orderId={}, reason={}",
                event.orderId(), event.reason());

        Order order = orderRepository.findById(UUID.fromString(event.orderId()))
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.orderId()));

        // Idempotent — skip if already cancelled
        if (order.getStatus() == OrderStatus.CANCELLED) {
            logger.warn("Order {} already CANCELLED, skipping", event.orderId());
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        logger.info("Order {} marked CANCELLED (inventory.failed)", event.orderId());

        rabbitTemplate.convertAndSend(
                EventRoutes.EXCHANGE,
                EventRoutes.ORDER_CANCELLED,
                new OrderCancelledEvent(event.orderId(), order.getUserId(), event.reason()));
    }
}
