package com.ecommerces.inventory.infrastructure.message_queue.rabbitmq;

import com.ecommerces.events.EventRoutes;
import com.ecommerces.events.InventoryFailedEvent;
import com.ecommerces.events.InventoryReservedEvent;
import com.ecommerces.events.OrderCreatedEvent;
import com.ecommerces.inventory.domain.ReservationStatus;
import com.ecommerces.inventory.domain.Stock;
import com.ecommerces.inventory.domain.StockReservation;
import com.ecommerces.inventory.ports.IInventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Listens for {@code order.created} events and attempts to reserve stock for
 * every item in the order.
 *
 * <p>Happy path: all items have sufficient stock →
 * stock quantities are decremented, {@link StockReservation} rows saved with
 * status {@code RESERVED}, and {@code inventory.reserved} is published.
 *
 * <p>Sad path: any item is out-of-stock →
 * all already-decremented quantities are restored, every reservation is saved
 * as {@code FAILED}, and {@code inventory.failed} is published.
 */
@Component
public class OrderCreatedListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final IInventoryRepository inventoryRepository;
    private final RabbitTemplate rabbitTemplate;

    public OrderCreatedListener(IInventoryRepository inventoryRepository,
            RabbitTemplate rabbitTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_CREATED)
    public void handleOrderCreated(OrderCreatedEvent event) {
        logger.info("Received order.created for orderId={}, items={}", event.orderId(),
                event.items().size());

        UUID orderId = UUID.fromString(event.orderId());
        List<StockReservation> reservations = new ArrayList<>();

        // ── 1. Try to reserve each item ─────────────────────────────────────────
        for (OrderCreatedEvent.OrderItemPayload item : event.items()) {
            UUID productId = UUID.fromString(item.productId());

            Stock stock = inventoryRepository.findStockByProductId(productId)
                    .orElse(null);

            if (stock == null || stock.getQuantity() < item.quantity()) {
                String reason = stock == null
                        ? "No stock record found for product " + item.productId()
                        : "Insufficient stock for product " + item.productId()
                                + ": requested " + item.quantity()
                                + ", available " + stock.getQuantity();

                logger.warn("Reservation failed for orderId={}: {}", event.orderId(), reason);

                // ── 2. Sad path: rollback all already-deducted quantities ────────
                rollbackReservations(reservations);

                // Save FAILED reservation records for audit trail
                for (OrderCreatedEvent.OrderItemPayload failedItem : event.items()) {
                    StockReservation failed = new StockReservation();
                    failed.setOrderId(orderId);
                    failed.setProductId(UUID.fromString(failedItem.productId()));
                    failed.setQuantity(failedItem.quantity());
                    failed.setStatus(ReservationStatus.FAILED);
                    inventoryRepository.saveReservation(failed);
                }

                rabbitTemplate.convertAndSend(
                        EventRoutes.EXCHANGE,
                        EventRoutes.INVENTORY_FAILED,
                        new InventoryFailedEvent(event.orderId(), reason));
                return;
            }

            // Deduct stock
            stock.setQuantity(stock.getQuantity() - item.quantity());
            inventoryRepository.saveStock(stock);

            // Record reservation
            StockReservation reservation = new StockReservation();
            reservation.setOrderId(orderId);
            reservation.setProductId(productId);
            reservation.setQuantity(item.quantity());
            reservation.setStatus(ReservationStatus.RESERVED);
            inventoryRepository.saveReservation(reservation);

            reservations.add(reservation);
        }

        // ── 3. Happy path: all items reserved ───────────────────────────────────
        logger.info("All items reserved for orderId={}", event.orderId());
        rabbitTemplate.convertAndSend(
                EventRoutes.EXCHANGE,
                EventRoutes.INVENTORY_RESERVED,
                new InventoryReservedEvent(event.orderId(), event.userId(), event.shopId()));
    }

    /**
     * Restores stock quantities for reservations that were already committed
     * in memory before a later item failed. Called only on the sad path.
     */
    private void rollbackReservations(List<StockReservation> committed) {
        for (StockReservation r : committed) {
            inventoryRepository.findStockByProductId(r.getProductId()).ifPresent(stock -> {
                stock.setQuantity(stock.getQuantity() + r.getQuantity());
                inventoryRepository.saveStock(stock);
            });
        }
    }
}
