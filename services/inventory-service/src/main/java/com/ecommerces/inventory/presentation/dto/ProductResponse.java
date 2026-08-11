package com.ecommerces.inventory.presentation.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for a Product enriched with live stock info.
 * {@code inStock} is derived: true when stockQuantity > 0.
 * Out-of-stock products still appear with inStock = false.
 */
public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        int stockQuantity,
        boolean inStock
) {
}
