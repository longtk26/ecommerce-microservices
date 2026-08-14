package com.ecommerces.inventory.presentation.controllers;

import com.ecommerces.inventory.presentation.dto.ProductResponse;
import com.ecommerces.inventory.usecases.GetProductsByIdsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Exposes product-level endpoints that are not scoped to a specific shop.
 *
 * <p>Used primarily by internal services (e.g., Order Service) that need to
 * fetch product information in bulk without knowing the shop they belong to.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final GetProductsByIdsUseCase getProductsByIdsUseCase;

    public ProductController(GetProductsByIdsUseCase getProductsByIdsUseCase) {
        this.getProductsByIdsUseCase = getProductsByIdsUseCase;
    }

    /**
     * GET /api/products?ids=uuid1,uuid2,...
     *
     * <p>Bulk-fetches product information for the given product IDs in a single
     * database query. Designed for internal service-to-service calls (e.g., from
     * the Order Service) to replace the N+1 per-product HTTP fetch pattern.
     *
     * @param ids comma-separated list of product UUIDs
     * @return list of matching {@link ProductResponse} objects (order not guaranteed)
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProductsByIds(
            @RequestParam("ids") List<UUID> ids) {
        return ResponseEntity.ok(getProductsByIdsUseCase.execute(ids));
    }
}
