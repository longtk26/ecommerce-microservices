package com.ecommerces.inventory.presentation.controllers;

import com.ecommerces.inventory.presentation.dto.ProductResponse;
import com.ecommerces.inventory.presentation.dto.ShopResponse;
import com.ecommerces.inventory.usecases.GetProductsByShopUseCase;
import com.ecommerces.inventory.usecases.GetShopsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shops")
public class ShopController {

    private final GetShopsUseCase getShopsUseCase;
    private final GetProductsByShopUseCase getProductsByShopUseCase;

    public ShopController(GetShopsUseCase getShopsUseCase,
                          GetProductsByShopUseCase getProductsByShopUseCase) {
        this.getShopsUseCase = getShopsUseCase;
        this.getProductsByShopUseCase = getProductsByShopUseCase;
    }

    /**
     * GET /api/shops
     * Returns all shops.
     */
    @GetMapping
    public ResponseEntity<List<ShopResponse>> getAllShops() {
        return ResponseEntity.ok(getShopsUseCase.execute());
    }

    /**
     * GET /api/shops/{shopId}/products
     * Returns all products for a shop enriched with live stock info.
     * Products with stockQuantity = 0 are included with inStock = false.
     */
    @GetMapping("/{shopId}/products")
    public ResponseEntity<List<ProductResponse>> getProductsByShop(@PathVariable UUID shopId) {
        return ResponseEntity.ok(getProductsByShopUseCase.execute(shopId));
    }
}
