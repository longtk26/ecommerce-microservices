package com.ecommerces.inventory.presentation.dto;

import java.util.UUID;

public record ShopResponse(
        UUID id,
        String name,
        String description,
        String logoUrl
) {
}
