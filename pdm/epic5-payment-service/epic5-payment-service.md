# Epic 5 — Payment Service (Mocked)

## Overview

The Payment Service simulates payment processing with a **configurable success/failure rate**. The real learning here is the **compensation event chain**: when payment fails, this service fires the event that triggers inventory rollback.

By keeping payment mocked you avoid real payment APIs and get precise control over failure scenarios — essential for testing rollback logic.

---

## 📐 Saga Role

```
                inventory.reserved
                       │
                       ▼
        ┌──────────────────────────────────┐
        │   PAYMENT SERVICE (port 8083)    │
        │                                  │
        │  Listens: inventory.reserved     │
        │    → Simulate payment            │
        │    → 80% success / 20% failure   │
        │    (configurable via env var)    │
        │                                  │
        │  Publishes:                      │
        │    payment.processed (success)   │
        │    payment.failed    (failure)   │
        └──────────────────────────────────┘
```

---

## 💳 Payment Simulation Logic

```java
// service/PaymentSimulator.java
@Component
public class PaymentSimulator {

    @Value("${payment.failure-rate:0.2}")
    private double failureRate;

    public PaymentResult simulate(BigDecimal amount) {
        // Add realistic processing delay
        try {
            long delay = 500 + (long)(Math.random() * 1000); // 500ms–1500ms
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean fails = Math.random() < failureRate;
        if (fails) {
            return PaymentResult.failure("Insufficient funds (simulated)");
        }
        return PaymentResult.success("txn_" + UUID.randomUUID());
    }
}

public record PaymentResult(boolean success, String transactionId, String failureReason) {
    public static PaymentResult success(String txnId) {
        return new PaymentResult(true, txnId, null);
    }
    public static PaymentResult failure(String reason) {
        return new PaymentResult(false, null, reason);
    }
}
```

> **Tip**: `payment.failure-rate` in `application.yml` can be overridden by the environment variable `PAYMENT_FAILURE_RATE`. Set it to `1.0` to force all failures (rollback testing) or `0.0` for all successes (happy path testing).

---

## 🖼️ Screen Flow

*This service has no UI — it's a background event consumer.*

---

## 📋 Stories

### Story 5.1 — Spring Boot Application Setup
**As a developer**, I want the Payment Service running and connected to its database.

**Acceptance Criteria:**
- [ ] Service starts on port `8083`
- [ ] Connects to `payments_db`, runs Flyway migrations on startup
- [ ] Connects to RabbitMQ via Spring AMQP
- [ ] `GET /actuator/health` returns `{ "status": "UP" }`
- [ ] `payment.failure-rate` property read from `application.yml` (default `0.2`)

**application.yml:**
```yaml
server:
  port: 8083

payment:
  failure-rate: ${PAYMENT_FAILURE_RATE:0.2}

spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:5432/payments_db
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASS}
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: 5672
    username: ${RABBITMQ_USER}
    password: ${RABBITMQ_PASS}
```

---

### Story 5.2 — Listen: inventory.reserved → Process Payment
**As a system**, when inventory is reserved, I want to attempt payment and record the result.

**Acceptance Criteria:**
- [ ] `@RabbitListener` on queue `payment-service.inventory-reserved`
- [ ] Deserializes `InventoryReservedEvent`
- [ ] Checks idempotency — if `order_id` already in `payment_attempts`, re-emit the stored result
- [ ] Calls `PaymentSimulator.simulate(totalAmount)`
  - Note: `totalAmount` must be included in `InventoryReservedEvent` or fetched from Order Service
- [ ] Records attempt in `payment_attempts` table
- [ ] On SUCCESS: publishes `payment.processed`
- [ ] On FAILURE: publishes `payment.failed`

**Listener Skeleton:**
```java
@Component
@Slf4j
public class InventoryReservedListener {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_INVENTORY_RESERVED)
    @Transactional
    public void handleInventoryReserved(InventoryReservedEvent event) {
        String orderId = event.orderId();
        log.info("Processing payment for orderId={}", orderId);

        // Idempotency: check if already processed
        Optional<PaymentAttempt> existing = paymentAttemptRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            log.warn("Payment already processed for orderId={}, re-emitting result", orderId);
            reEmitResult(existing.get());
            return;
        }

        PaymentResult result = paymentSimulator.simulate(event.totalAmount());

        PaymentAttempt attempt = new PaymentAttempt(
            orderId,
            event.totalAmount(),
            result.success() ? "SUCCESS" : "FAILED",
            result.transactionId(),
            result.failureReason()
        );
        paymentAttemptRepository.save(attempt);

        if (result.success()) {
            log.info("Payment SUCCESS for orderId={}, txn={}", orderId, result.transactionId());
            rabbitTemplate.convertAndSend(EventRoutes.EXCHANGE, EventRoutes.PAYMENT_PROCESSED,
                new PaymentProcessedEvent(orderId, event.totalAmount(), result.transactionId()));
        } else {
            log.warn("Payment FAILED for orderId={}, reason={}", orderId, result.failureReason());
            rabbitTemplate.convertAndSend(EventRoutes.EXCHANGE, EventRoutes.PAYMENT_FAILED,
                new PaymentFailedEvent(orderId, result.failureReason()));
        }
    }
}
```

> ⚠️ **Note on totalAmount**: The `InventoryReservedEvent` needs to carry `totalAmount` so the Payment Service doesn't need to call the Order Service. Update the event DTO in `shared-events` to include this field.

---

### Story 5.3 — Configurable Failure Rate
**As a developer**, I want to easily control the failure rate for testing different scenarios.

**Acceptance Criteria:**
- [ ] `PAYMENT_FAILURE_RATE=1.0` in `.env` → all payments fail (rollback testing)
- [ ] `PAYMENT_FAILURE_RATE=0.0` in `.env` → all payments succeed (happy path testing)
- [ ] Default is `0.2` (20% failure)
- [ ] No code changes required — only environment variable change

---

## ✅ Epic 5 Definition of Done

- [ ] `inventory.reserved` triggers payment simulation
- [ ] ~80% of payments succeed → `payment.processed` published
- [ ] ~20% of payments fail → `payment.failed` published
- [ ] Result recorded in `payment_attempts` table
- [ ] All listeners are idempotent
- [ ] `PAYMENT_FAILURE_RATE=1.0` forces all failures
- [ ] `PAYMENT_FAILURE_RATE=0.0` forces all successes
