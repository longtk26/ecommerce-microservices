package com.ecommerces.inventory.infrastructure.message_queue.rabbitmq;

import com.ecommerces.events.PaymentProcessedEvent;
import com.ecommerces.inventory.domain.ReservationStatus;
import com.ecommerces.inventory.domain.StockReservation;
import com.ecommerces.inventory.ports.IInventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Listens for {@code payment.processed} events and marks the stock reservations
 * for that order as {@code RELEASED} (i.e. consumed / fulfilled).
 *
 * <p>Stock quantities were already decremented at reservation time, so no
 * further stock change is needed here — we only update the reservation audit
 * rows so downstream queries can distinguish "used" from "still on hold".
 */
@Component
public class PaymentProcessedListener {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessedListener.class);

    private final IInventoryRepository inventoryRepository;

    public PaymentProcessedListener(IInventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_PROCESSED)
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        logger.info("Received payment.processed for orderId={}", event.orderId());

        UUID orderId = UUID.fromString(event.orderId());
        List<StockReservation> reservations = inventoryRepository.findReservationsByOrderId(orderId);

        if (reservations.isEmpty()) {
            logger.warn("No reservations found for orderId={} — skipping", event.orderId());
            return;
        }

        for (StockReservation reservation : reservations) {
            // Idempotent — skip already-released reservations
            if (reservation.getStatus() == ReservationStatus.RELEASED) {
                logger.debug("Reservation {} already RELEASED, skipping", reservation.getId());
                continue;
            }
            reservation.setStatus(ReservationStatus.RELEASED);
            inventoryRepository.saveReservation(reservation);
        }

        logger.info("Marked {} reservation(s) as RELEASED for orderId={}",
                reservations.size(), event.orderId());
    }
}
