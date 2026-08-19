package com.ecommerces.inventory.infrastructure.persistence;

import com.ecommerces.inventory.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface StockJpaRepository extends JpaRepository<Stock, UUID> {

    Optional<Stock> findByProductId(UUID productId);

    @Query("SELECT s FROM Stock s WHERE s.product.id IN :productIds")
    List<Stock> findByProductIdIn(@Param("productIds") List<UUID> productIds);
}
