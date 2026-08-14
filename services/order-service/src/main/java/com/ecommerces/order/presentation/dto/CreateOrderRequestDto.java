package com.ecommerces.order.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class CreateOrderRequestDto {

    @NotBlank
    private String userId;

    /** UUID string — validated format is enforced in the use case during UUID.fromString(). */
    @NotBlank
    private String shopId;

    @NotEmpty
    @Valid
    private List<OrderItemDto> items;
}