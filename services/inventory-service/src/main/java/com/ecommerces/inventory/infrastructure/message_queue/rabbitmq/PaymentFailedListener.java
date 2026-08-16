package com.ecommerces.inventory.infrastructure.message_queue.rabbitmq;

import com.ecommerces.events.EventRoutes;
import com.ecommerces.events.InventoryReleasedEvent;
import com.ecommerces.events.PaymentFailedEvent;
import com.ecommerces.inventory.domain.ReservationStatus;
import com.ecommerces.inventory.domain.StockReservation;
import com.ecommerces.inventory.ports.IInventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Listens for {@code payment.failed} events and releases (restores) the stock
 * that was held for the order.
 *
 * <p>Compensation flow:
 * <pre>
 *   payment.failed
 *         ↓
 *   For each RESERVED reservation:
 *     stock.quantity += reservation.quantity   ← restore to available pool
 *     reservation.status = RELEASED            ← mark as returned
 *         ↓
 *   Publish inventory.released
 *         ↓
 *   Order Service cancels the order
 * </pre>
 *
 * <p>Idempotent: if all reservations are already {@code RELEASED}, the event
 * is re-published so the Order Service can always reach CANCELLED even on retry.
 */
@Component
public class PaymentFailedListener {

    private static final Logger logger = LoggerFactory.getLogger(PaymentFailedListener.class);

    private final IInventoryRepository inventoryRepository;
    private final RabbitTemplate rabbitTemplate;

    public PaymentFailedListener(IInventoryRepository inventoryRepository,
            RabbitTemplate rabbitTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_FAILED)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        logger.info("Received payment.failed for orderId={}, reason={}",
                event.orderId(), event.reason());

        UUID orderId = UUID.fromString(event.orderId());
        List<StockReservation> reservations = inventoryRepository.findReservationsByOrderId(orderId);

        if (reservations.isEmpty()) {
            logger.warn("No reservations found for orderId={} — skipping", event.orderId());
            return;
        }

        int restoredCount = 0;
        for (StockReservation reservation : reservations) {
            // Idempotent — skip anything that isn't RESERVED (already processed)
            if (reservation.getStatus() != ReservationStatus.RESERVED) {
                logger.debug("Reservation {} has status {} — skipping",
                        reservation.getId(), reservation.getStatus());
                continue;
            }

            // Restore stock quantity back to available pool
            inventoryRepository.findStockByProductId(reservation.getProductId())
                    .ifPresentOrElse(
                            stock -> {
                                stock.setQuantity(stock.getQuantity() + reservation.getQuantity());
                                inventoryRepository.saveStock(stock);
                            },
                            () -> logger.warn("Stock not found for productId={} — quantity not restored",
                                    reservation.getProductId()));

            // Mark reservation as released (returned to available inventory)
            reservation.setStatus(ReservationStatus.RELEASED);
            inventoryRepository.saveReservation(reservation);
            restoredCount++;
        }

        logger.info("Released {} reservation(s) for orderId={}, stock restored", restoredCount,
                event.orderId());

        // Notify order-service to cancel the order
        rabbitTemplate.convertAndSend(
                EventRoutes.EXCHANGE,
                EventRoutes.INVENTORY_RELEASED,
                new InventoryReleasedEvent(event.orderId()));
    }
}
