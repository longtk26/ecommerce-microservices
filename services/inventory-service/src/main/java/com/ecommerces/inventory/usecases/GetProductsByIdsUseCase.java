package com.ecommerces.inventory.usecases;

import com.ecommerces.inventory.domain.Product;
import com.ecommerces.inventory.domain.Stock;
import com.ecommerces.inventory.ports.IInventoryRepository;
import com.ecommerces.inventory.presentation.dto.ProductResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Bulk-fetches products by their IDs in a single repository query,
 * avoiding the N+1 HTTP call pattern that occurs when fetching products
 * one-by-one during order creation.
 */
@Service
@Transactional(readOnly = true)
public class GetProductsByIdsUseCase {
    private static final Logger logger = LoggerFactory.getLogger(GetProductsByIdsUseCase.class);

    private final IInventoryRepository inventoryRepository;

    public GetProductsByIdsUseCase(IInventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * @param productIds list of product UUIDs to fetch
     * @return list of {@link ProductResponse} for the matching products (order not
     *         guaranteed)
     */
    public List<ProductResponse> execute(List<UUID> productIds) {
        List<Product> products = inventoryRepository.findProductsByIds(productIds);
        logger.info("Found {} products", products.size());
        return products.stream()
                .map(this::toResponse)
                .toList();
    }

    private ProductResponse toResponse(Product product) {
        int qty = inventoryRepository.findStockByProductId(product.getId())
                .map(Stock::getQuantity)
                .orElse(0);

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                qty,
                qty > 0);
    }
}
