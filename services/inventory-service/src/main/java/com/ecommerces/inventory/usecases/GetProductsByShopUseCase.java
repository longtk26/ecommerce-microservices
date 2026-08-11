package com.ecommerces.inventory.usecases;

import com.ecommerces.inventory.domain.Product;
import com.ecommerces.inventory.domain.Stock;
import com.ecommerces.inventory.ports.IInventoryRepository;
import com.ecommerces.inventory.presentation.dto.ProductResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetProductsByShopUseCase {

    private final IInventoryRepository inventoryRepository;

    public GetProductsByShopUseCase(IInventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public List<ProductResponse> execute(UUID shopId) {
        List<Product> products = inventoryRepository.findProductsByShopId(shopId);
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
                qty > 0
        );
    }
}
