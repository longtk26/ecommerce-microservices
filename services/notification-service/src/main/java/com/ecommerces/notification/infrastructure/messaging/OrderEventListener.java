package com.ecommerces.notification.infrastructure.messaging;

import com.ecommerces.events.EventRoutes;
import com.ecommerces.events.OrderCancelledEvent;
import com.ecommerces.events.OrderCompletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Observer — listens to all {@code order.*} events and logs mock emails.
 * This class has zero knowledge of Order Service, Inventory Service, or
 * Payment Service internals. It simply reacts to published events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDERS)
    public void handleOrderEvent(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        switch (routingKey) {
            case EventRoutes.ORDER_COMPLETED -> handleCompleted(body);
            case EventRoutes.ORDER_CANCELLED -> handleCancelled(body);
            default -> log.debug("Unhandled order event: {}", routingKey);
        }
    }

    // ── Story 6.2 — Success notification ──────────────────────────────────────

    private void handleCompleted(String body) {
        OrderCompletedEvent event = deserialize(body, OrderCompletedEvent.class);
        log.info("""
                \n📧 Mock Email Sent
                  To:      {}@example.com
                  Subject: Your order is COMPLETE! 🎉
                  Body:    Hi! Your order #{} for ${} has been processed successfully.
                           Your items are on their way!
                """,
                event.userId(), event.orderId(), event.totalAmount()
        );
    }

    // ── Story 6.3 — Cancellation notification with contextual reason ──────────

    private void handleCancelled(String body) {
        OrderCancelledEvent event = deserialize(body, OrderCancelledEvent.class);

        String reasonMessage = switch (event.reason()) {
            case "Item out of stock" ->
                    "We're sorry — this item is no longer available.";
            case "Insufficient funds (simulated)" ->
                    "Your payment could not be processed.";
            default ->
                    "Your order could not be completed. Reason: " + event.reason();
        };

        log.info("""
                \n📧 Mock Email Sent
                  To:      {}@example.com
                  Subject: Your order has been cancelled
                  Body:    Hi! Your order #{} was cancelled.
                           {}
                           No charges were made.
                """,
                event.userId(), event.orderId(), reasonMessage
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize event payload into " + type.getSimpleName(), e);
        }
    }
}
