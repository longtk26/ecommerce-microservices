package com.ecommerces.events;

import java.math.BigDecimal;

/**
 * Published by the Order Service after payment is confirmed.
 * Consumed by: Inventory Service (to release reservations), Notification
 * Service.
 */
public record OrderCompletedEvent(
        String orderId,
        String userId,
        BigDecimal totalAmount) {
}
