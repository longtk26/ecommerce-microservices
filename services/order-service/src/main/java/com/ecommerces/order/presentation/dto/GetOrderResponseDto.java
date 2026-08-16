package com.ecommerces.order.presentation.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response body for GET /api/orders/{orderId} (internal use by other services).
 * Contains the minimal fields needed for downstream processing (e.g. payment-service).
 */
public record GetOrderResponseDto(
        String orderId,
        String userId,
        String shopId,
        String status,
        BigDecimal totalAmount,
        List<GetOrderItemDto> items) {

    public record GetOrderItemDto(
            String productId,
            String productName,
            BigDecimal unitPrice,
            int quantity) {
    }
}
