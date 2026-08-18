package com.ecommerces.inventory.ports;

import com.ecommerces.inventory.domain.Product;
import com.ecommerces.inventory.domain.Shop;
import com.ecommerces.inventory.domain.Stock;
import com.ecommerces.inventory.domain.StockReservation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IInventoryRepository {

    List<Shop> findAllShops();

    List<Product> findProductsByShopId(UUID shopId);

    /** Bulk-fetch products whose IDs are in the given list (single query). */
    List<Product> findProductsByIds(List<UUID> productIds);

    Optional<Product> findProductById(UUID productId);

    Optional<Stock> findStockByProductId(UUID productId);

    List<Stock> findStocksByProductIds(List<UUID> productIds);

    Stock saveStock(Stock stock);

    List<Stock> saveAllStocks(List<Stock> stocks);

    StockReservation saveReservation(StockReservation reservation);

    List<StockReservation> saveReservations(List<StockReservation> reservations);

    List<StockReservation> findReservationsByOrderId(UUID orderId);
}
