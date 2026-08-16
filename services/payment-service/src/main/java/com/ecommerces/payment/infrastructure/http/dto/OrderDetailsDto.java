package com.ecommerces.payment.infrastructure.http.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Internal DTO representing the order details fetched from the order-service.
 * Mirrors {@code GetOrderResponseDto} in the order-service but belongs to the
 * payment-service's own port layer — keeps the two services decoupled.
 */
public record OrderDetailsDto(
                String orderId,
                String userId,
                String shopId,
                String status,
                BigDecimal totalAmount,
                List<OrderItemPayload> items) {

        public record OrderItemPayload(
                        String productId,
                        String productName,
                        BigDecimal unitPrice,
                        int quantity) {
        }
}
