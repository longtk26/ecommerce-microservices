package com.ecommerces.inventory.infrastructure.persistence;

import com.ecommerces.inventory.domain.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ShopJpaRepository extends JpaRepository<Shop, UUID> {
}
