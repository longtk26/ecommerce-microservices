package com.ecommerces.inventory.infrastructure.persistence;

import com.ecommerces.inventory.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface ProductJpaRepository extends JpaRepository<Product, UUID> {

    @Query("""
            SELECT p FROM Product p
            WHERE p.shopId = :shopId
            ORDER BY p.createdAt ASC
            """)
    List<Product> findByShopId(@Param("shopId") UUID shopId);

    /**
     * Bulk-fetch products whose IDs are in the given collection.
     * JpaRepository.findAllById() already generates an IN-clause query, but this
     * explicit alias keeps the naming consistent with the port interface.
     */
    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    List<Product> findByIdIn(@Param("ids") List<UUID> ids);
}
