package com.ecommerces.events;

import java.math.BigDecimal;
import java.util.List;

/**
 * Published by the Order Service after a new order is successfully persisted.
 * This is the first event in the saga choreography chain.
 * Consumed by: Inventory Service (to reserve stock).
 */
public record OrderCreatedEvent(
        String orderId,
        String userId,
        String shopId,
        List<OrderItemPayload> items,
        BigDecimal totalAmount
) {

    /**
     * Snapshot of a single line item at the moment the order was placed.
     * Prices and names are frozen here so downstream services don't need to
     * look them up again.
     */
    public record OrderItemPayload(
            String productId,
            String productName,
            BigDecimal unitPrice,
            int quantity
    ) {}
}
