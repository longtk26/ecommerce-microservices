package com.ecommerces.inventory.infrastructure.persistence;

import com.ecommerces.inventory.domain.Product;
import com.ecommerces.inventory.domain.Shop;
import com.ecommerces.inventory.domain.Stock;
import com.ecommerces.inventory.domain.StockReservation;
import com.ecommerces.inventory.ports.IInventoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class InventoryRepositoryAdapter implements IInventoryRepository {

    private final ShopJpaRepository shopJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final StockJpaRepository stockJpaRepository;
    private final StockReservationJpaRepository reservationJpaRepository;

    public InventoryRepositoryAdapter(ShopJpaRepository shopJpaRepository,
                                      ProductJpaRepository productJpaRepository,
                                      StockJpaRepository stockJpaRepository,
                                      StockReservationJpaRepository reservationJpaRepository) {
        this.shopJpaRepository = shopJpaRepository;
        this.productJpaRepository = productJpaRepository;
        this.stockJpaRepository = stockJpaRepository;
        this.reservationJpaRepository = reservationJpaRepository;
    }

    @Override
    public List<Shop> findAllShops() {
        return shopJpaRepository.findAll();
    }

    @Override
    public List<Product> findProductsByShopId(UUID shopId) {
        return productJpaRepository.findByShopId(shopId);
    }

    @Override
    public List<Product> findProductsByIds(List<UUID> productIds) {
        return productJpaRepository.findByIdIn(productIds);
    }

    @Override
    public Optional<Product> findProductById(UUID productId) {
        return productJpaRepository.findById(productId);
    }

    @Override
    public Optional<Stock> findStockByProductId(UUID productId) {
        return stockJpaRepository.findByProductId(productId);
    }

    @Override
    public Stock saveStock(Stock stock) {
        return stockJpaRepository.save(stock);
    }

    @Override
    public StockReservation saveReservation(StockReservation reservation) {
        return reservationJpaRepository.save(reservation);
    }

    @Override
    public List<StockReservation> findReservationsByOrderId(UUID orderId) {
        return reservationJpaRepository.findByOrderId(orderId);
    }
}
