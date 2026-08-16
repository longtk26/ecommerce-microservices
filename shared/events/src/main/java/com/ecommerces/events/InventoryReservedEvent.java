package com.ecommerces.events;

/**
 * Published by the Inventory Service after all items in an order have been
 * successfully reserved. This unblocks the Payment Service to proceed with
 * charging the customer.
 */
public record InventoryReservedEvent(
        String orderId,
        String userId,
        String shopId) {
}
