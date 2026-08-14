package com.ecommerces.order.presentation.dto;

/**
 * Response body for POST /api/orders (201 Created).
 * The order is immediately returned in PENDING state; the frontend should poll
 * GET /api/orders/{orderId} to get the final status.
 */
public record CreateOrderResponseDto(
        String orderId,
        String status,
        String message
) {}
