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
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Listens for {@code order.created} events and attempts to reserve stock for
 * every item in the order.
 *
 * <p>
 * Happy path: all items have sufficient stock →
 * stock quantities are decremented, {@link StockReservation} rows saved with
 * status {@code RESERVED}, and {@code inventory.reserved} is published.
 *
 * <p>
 * Sad path: any item is out-of-stock →
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
        List<UUID> productIds = event.items().stream().map(item -> UUID.fromString(item.productId())).toList();
        List<Stock> stocks = inventoryRepository.findStocksByProductIds(productIds);
        Map<UUID, Stock> stockMap = stocks.stream()
                .collect(Collectors.toMap(stock -> stock.getProduct().getId(), Function.identity()));

        // ── Validate Order Items ─────────────────────────────────────────
        boolean isSuccess = validateOrderItems(event, stockMap);
        if (!isSuccess) {
            return;
        }

        // ── Reserve Stock ─────────────────────────────────────────
        List<StockReservation> reservations = new ArrayList<>();

        for (OrderCreatedEvent.OrderItemPayload item : event.items()) {
            UUID productId = UUID.fromString(item.productId());
            int quantity = item.quantity();
            Stock stock = stockMap.get(productId);

            stock.setQuantity(stock.getQuantity() - quantity);

            StockReservation reservation = new StockReservation(
                    orderId,
                    productId,
                    quantity,
                    ReservationStatus.RESERVED);

            reservations.add(reservation);
        }
        inventoryRepository.saveReservations(reservations);

        // List<Stock> committedStocks = stockMap.values().stream()
        // .filter(s -> s.getQuantity() >= 0)
        // .toList();
        // inventoryRepository.saveAllStocks(committedStocks);

        // ── Send Event ─────────────────────────────────────────
        logger.info("All items reserved for orderId={}", event.orderId());
        rabbitTemplate.convertAndSend(
                EventRoutes.EXCHANGE,
                EventRoutes.INVENTORY_RESERVED,
                new InventoryReservedEvent(event.orderId(), event.userId(), event.shopId()));
    }

    private boolean validateOrderItems(OrderCreatedEvent event, Map<UUID, Stock> stockMap) {
        try {
            for (OrderCreatedEvent.OrderItemPayload item : event.items()) {
                UUID productId = UUID.fromString(item.productId());
                int quantity = item.quantity();
                Stock stock = stockMap.get(productId);
                if (stock == null || stock.getQuantity() < quantity) {
                    throw new RuntimeException("Stock not available for product " + productId);
                }
            }

        } catch (RuntimeException e) {
            logger.warn(
                    "Stock unavailable for orderId={}, reason={}",
                    event.orderId(),
                    e.getMessage());
            rabbitTemplate.convertAndSend(
                    EventRoutes.EXCHANGE,
                    EventRoutes.INVENTORY_FAILED,
                    new InventoryFailedEvent(
                            event.orderId(),
                            e.getMessage()));
            return false;
        }
        return true;
    }

}
