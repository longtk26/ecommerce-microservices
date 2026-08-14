package com.ecommerces.order.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class OrderItemDto {

    @NotBlank
    private String productId;

    /** Must be at least 1 — zero-quantity line items are not meaningful. */
    @Min(1)
    private int quantity;
}
