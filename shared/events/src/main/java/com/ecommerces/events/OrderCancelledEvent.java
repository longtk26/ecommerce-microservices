package com.ecommerces.events;

/**
 * Published by the Order Service after an order is cancelled (due to inventory
 * failure or inventory release after payment failure).
 * Consumed by: Notification Service.
 */
public record OrderCancelledEvent(
        String orderId,
        String userId,
        String reason) {
}
