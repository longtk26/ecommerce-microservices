# Epic 4 — Inventory Service

## Overview

The Inventory Service is the **most complex service in this project**. It must solve:
1. **Concurrent stock deduction** — 10 orders arriving simultaneously for 3 items
2. **Compensation** — restoring stock when payment fails (rollback)
3. **Idempotency** — never deduct stock twice for the same order

This is where JPA's `@Version` (Optimistic Locking) earns its keep.

---

## 📐 Saga Role

```
                    order.created
                         │
                         ▼
          ┌──────────────────────────────────┐
          │   INVENTORY SERVICE (port 8082)  │
          │                                  │
          │  Listens: order.created          │
          │    → Try to reserve stock        │
          │    → @Version optimistic lock    │
          │                                  │
          │  Listens: payment.failed         │
          │    → Restore stock (COMPENSATION)│
          │    → Publish inventory.released  │
          │                                  │
          │  Publishes:                      │
          │    inventory.reserved            │
          │    inventory.failed              │
          │    inventory.released            │
          └──────────────────────────────────┘
```

---

## 🔒 The Concurrency Problem (Critical Understanding)

**The Scenario:**
- USB-C Hub has `quantity = 3, version = 1`
- 10 simultaneous orders each want `quantity = 1`
- Without locking: all 10 read `quantity=3`, all think they can proceed → `quantity=-7` 💥

**The Solution: JPA `@Version` (Optimistic Locking)**

When multiple transactions try to update the same `Stock` entity:
```
Thread 1: READ  stock (quantity=3, version=1) → UPDATE SET quantity=2, version=2 ✅ SUCCESS
Thread 2: READ  stock (quantity=3, version=1) → UPDATE fails (version mismatch) → OptimisticLockException ❌
Thread 3–10: same as Thread 2 → all fail ❌
```

Spring Data JPA automatically handles `@Version` — you just annotate the field and catch `OptimisticLockingFailureException`.

```java
@Entity
public class Stock {
    @Version
    private int version;  // JPA manages this automatically
    private int quantity;
}
```

When a save fails with `OptimisticLockingFailureException`, it means someone else reserved stock first — publish `inventory.failed`.

> **Why NOT pessimistic locking (`PESSIMISTIC_WRITE`)?**  
> Pessimistic locking holds a DB row lock while your transaction runs. Under high concurrency, threads queue up waiting for the lock — this creates latency spikes and timeouts. Optimistic locking fails fast (no blocking) — much better for saga patterns.

---

## 🖼️ Screen Flow

*This service exposes REST for product browsing (Epic 2) and acts as an event consumer for the saga.*

---

## 📋 Stories

### Story 4.1 — Spring Boot Application Setup
**As a developer**, I want the Inventory Service running and connected to its database.

**Acceptance Criteria:**
- [ ] Service starts on port `8082`
- [ ] Connects to `inventory_db`, runs Flyway migrations on startup
- [ ] Connects to RabbitMQ via Spring AMQP
- [ ] `GET /actuator/health` returns `{ "status": "UP" }`
- [ ] `GET /api/shops` and `GET /api/shops/{shopId}/products` work (from Epic 2)

---

### Story 4.2 — Listen: order.created → Reserve Stock
**As a system**, when an order is created, I want to atomically reserve the requested stock.

**Acceptance Criteria:**
- [ ] `@RabbitListener` on queue `inventory-service.order-created`
- [ ] For each item in the order, attempts to decrement `Stock.quantity`
- [ ] Uses JPA optimistic locking — catches `OptimisticLockingFailureException`
- [ ] If ALL items succeed: records reservation, publishes `inventory.reserved`
- [ ] If ANY item fails (out of stock OR version conflict):
  - Rolls back already-deducted items using the `Reservation` table
  - Publishes `inventory.failed` with reason
- [ ] Idempotent: checks `Reservation` table for existing `orderId` before processing

**Reservations Table (also created via Flyway):**
```sql
-- V4__create_reservations.sql
CREATE TABLE IF NOT EXISTS reservations (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id   UUID NOT NULL UNIQUE,
    items_json TEXT NOT NULL,        -- JSON snapshot of what was reserved
    status     VARCHAR(20) NOT NULL DEFAULT 'RESERVED',   -- RESERVED | RELEASED
    created_at TIMESTAMP DEFAULT NOW()
);
```

**Service Skeleton:**
```java
@Service
@Slf4j
public class StockReservationService {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_CREATED)
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        String orderId = event.orderId();
        log.info("Handling order.created for orderId={}", orderId);

        // Idempotency check
        if (reservationRepository.existsByOrderId(orderId)) {
            log.warn("Reservation already exists for orderId={}, skipping", orderId);
            return;
        }

        List<OrderItem> reserved = new ArrayList<>();
        try {
            for (OrderItem item : event.items()) {
                Stock stock = stockRepository
                    .findByProductIdWithLock(UUID.fromString(item.productId()))
                    .orElseThrow(() -> new ProductNotFoundException(item.productId()));

                if (stock.getQuantity() < item.quantity()) {
                    throw new InsufficientStockException(item.productId());
                }

                stock.setQuantity(stock.getQuantity() - item.quantity());
                stockRepository.save(stock);   // @Version check happens here
                reserved.add(item);
            }

            // All items reserved — persist and publish
            reservationRepository.save(new Reservation(orderId, event.items(), "RESERVED"));
            rabbitTemplate.convertAndSend(EventRoutes.EXCHANGE, EventRoutes.INVENTORY_RESERVED,
                new InventoryReservedEvent(orderId, event.items()));

        } catch (OptimisticLockingFailureException | InsufficientStockException e) {
            // Rollback already-reserved items
            rollbackReservations(reserved);
            rabbitTemplate.convertAndSend(EventRoutes.EXCHANGE, EventRoutes.INVENTORY_FAILED,
                new InventoryFailedEvent(orderId, e.getMessage()));
        }
    }
}
```

> ⚠️ **Important**: The `@Transactional` boundary matters here. The optimistic lock exception is thrown by JPA on `save()`. If you catch it inside the same transaction, the transaction is already marked for rollback — you need to handle the compensation in a **new** transaction (use `@Transactional(propagation = REQUIRES_NEW)` for the compensation logic or publish the failure event after the transaction rolls back).

---

### Story 4.3 — Listen: payment.failed → Restore Stock (Compensation)
**As a system**, when payment fails, I want to restore the previously reserved stock.

**Acceptance Criteria:**
- [ ] `@RabbitListener` on queue `inventory-service.payment-failed`
- [ ] Looks up the `Reservation` record for the `orderId`
- [ ] Restores stock for each item: `quantity += reservedQty`
  - No version check needed — adding back stock cannot cause overselling
- [ ] Marks the `Reservation` as `RELEASED`
- [ ] Publishes `inventory.released`
- [ ] Idempotent: if reservation already `RELEASED`, skip

**Service Skeleton:**
```java
@RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_FAILED)
@Transactional
public void handlePaymentFailed(PaymentFailedEvent event) {
    String orderId = event.orderId();
    log.info("Handling payment.failed for orderId={} — restoring stock", orderId);

    Reservation reservation = reservationRepository.findByOrderId(orderId)
        .orElseThrow(() -> new ReservationNotFoundException(orderId));

    if ("RELEASED".equals(reservation.getStatus())) {
        log.warn("Reservation for orderId={} already released, skipping", orderId);
        return; // idempotent
    }

    // Restore each item's stock
    for (OrderItem item : reservation.getItems()) {
        Stock stock = stockRepository.findByProductId(UUID.fromString(item.productId()))
            .orElseThrow();
        stock.setQuantity(stock.getQuantity() + item.quantity());
        stockRepository.save(stock);
    }

    reservation.setStatus("RELEASED");
    reservationRepository.save(reservation);

    rabbitTemplate.convertAndSend(EventRoutes.EXCHANGE, EventRoutes.INVENTORY_RELEASED,
        new InventoryReleasedEvent(orderId));
}
```

---

## ✅ Epic 4 Definition of Done

- [ ] `order.created` triggers stock deduction with optimistic locking (`@Version`)
- [ ] Out-of-stock orders receive `inventory.failed` event
- [ ] `payment.failed` triggers stock restoration and `inventory.released`
- [ ] 10 concurrent orders for a 3-stock product: exactly correct number succeed, rest fail
- [ ] All listeners are idempotent
- [ ] `Reservation` table tracks what was reserved for compensation
