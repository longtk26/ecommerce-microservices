package com.ecommerces.order.infrastructure.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Projection of the Inventory Service's product response, containing only
 * the fields needed to build the price snapshot in an order item.
 *
 * <p>Mapped from: {@code GET /api/products?ids=...} (bulk) or {@code GET /api/products/{id}}.
 * The inventory service returns {@code id}, {@code name}, and {@code price};
 * {@link JsonProperty} bridges those names to the order-side field names.
 */
public record ProductInfo(
        @JsonProperty("id")    String productId,
        @JsonProperty("name")  String productName,
        @JsonProperty("price") BigDecimal unitPrice
) {}

