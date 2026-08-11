package com.ecommerces.inventory.usecases;

import com.ecommerces.inventory.domain.Shop;
import com.ecommerces.inventory.ports.IInventoryRepository;
import com.ecommerces.inventory.presentation.dto.ShopResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetShopsUseCase {

    private final IInventoryRepository inventoryRepository;

    public GetShopsUseCase(IInventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public List<ShopResponse> execute() {
        return inventoryRepository.findAllShops().stream()
                .map(this::toResponse)
                .toList();
    }

    private ShopResponse toResponse(Shop shop) {
        return new ShopResponse(
                shop.getId(),
                shop.getName(),
                shop.getDescription(),
                shop.getLogoUrl());
    }
}
