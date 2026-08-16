package com.ecommerces.events;

/**
 * Published by the Payment Service when a payment attempt fails.
 * Triggers the Inventory Service to release (restore) the reserved stock
 * and publish {@code inventory.released} so the Order Service can cancel the order.
 */
public record PaymentFailedEvent(
        String orderId,
        String reason) {
}
