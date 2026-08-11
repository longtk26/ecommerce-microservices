package com.ecommerces.inventory.infrastructure.persistence;

import com.ecommerces.inventory.domain.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface StockReservationJpaRepository extends JpaRepository<StockReservation, UUID> {

    List<StockReservation> findByOrderId(UUID orderId);
}
