package com.ecommerces.events;

/**
 * Published by the Inventory Service after reserved stock is restored to the
 * available pool (i.e. payment failed, so the held units are put back).
 * Triggers the Order Service to cancel the order.
 */
public record InventoryReleasedEvent(String orderId) {
}
