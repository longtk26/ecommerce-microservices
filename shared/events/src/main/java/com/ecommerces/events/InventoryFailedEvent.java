package com.ecommerces.events;

/**
 * Published by the Inventory Service when stock reservation fails for one or more
 * items in an order. Triggers the Order Service to cancel the order.
 */
public record InventoryFailedEvent(
        String orderId,
        String reason) {
}
