package com.ecommerces.events;

import java.math.BigDecimal;
import java.util.List;

public record PaymentProcessedEvent(
        String orderId,
        String userId,
        String shopId,
        List<OrderItemPayload> items,
        BigDecimal totalAmount) {

    public record OrderItemPayload(
            String productId,
            String productName,
            BigDecimal unitPrice,
            int quantity) {
    }
}
