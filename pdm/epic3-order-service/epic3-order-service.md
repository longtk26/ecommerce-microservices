# Epic 3 — Order Service

## Overview

The Order Service is the **entry point of the saga**. It receives checkout requests from the frontend, persists the order in a `PENDING` state, and publishes the first event that kicks off the entire choreography chain.

It also listens for the final outcome events to update the order's status to `COMPLETED` or `CANCELLED`.

**This service never calls another service directly.** It only speaks through events.

---

## 📐 Saga Role

```
         ┌──────────────────────────────────────────────┐
USER ───► │          ORDER SERVICE (port 8081)           │
         │                                              │
         │  1. POST /api/orders → INSERT status=PENDING │
         │  2. Publish: order.created                   │
         │                                              │
         │  Later, listens for:                         │
         │  - payment.processed → UPDATE COMPLETED      │
         │  - inventory.failed  → UPDATE CANCELLED      │
         │  - inventory.released→ UPDATE CANCELLED      │
         └──────────────────────────────────────────────┘
                              │
                              │ order.created (event)
                              ▼
                      Inventory Service
```

---

## 🖼️ API Contract

### POST /api/orders

**Request Body:**
```json
{
  "userId": "user-john",
  "shopId": "a1b2c3d4-0001-0001-0001-000000000001",
  "items": [
    { "productId": "prod-0001-0001-0001-000000000001", "quantity": 2 }
  ]
}
```

**Response (201 Created):**
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "message": "Order placed. Processing..."
}
```

> The order is immediately returned as `PENDING`. The frontend polls `GET /api/orders/{id}` to get the final status.

### GET /api/orders/{orderId}

**Response:**
```json
{
  "orderId": "550e8400-...",
  "status": "COMPLETED",
  "totalAmount": 59.98,
  "items": [
    { "productName": "USB-C Hub 7-in-1", "unitPrice": 29.99, "quantity": 2 }
  ],
  "createdAt": "2026-01-01T10:00:00"
}
```

---

## 📋 Stories

### Story 3.1 — Spring Boot Application Setup
**As a developer**, I want a running Spring Boot application for the Order Service.

**Acceptance Criteria:**
- [ ] `services/order-service/` has a valid `pom.xml` with Spring Web, AMQP, Data JPA, PostgreSQL, Flyway
- [ ] `application.yml` reads DB + RabbitMQ credentials from environment variables
- [ ] Connects to `orders_db` and runs Flyway migrations on startup
- [ ] `GET /actuator/health` returns `{ "status": "UP" }` (Spring Actuator)
- [ ] Service starts cleanly with `./mvnw spring-boot:run`

**application.yml:**
```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:5432/orders_db
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASS}
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: 5672
    username: ${RABBITMQ_USER}
    password: ${RABBITMQ_PASS}
  flyway:
    enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health
```

---

### Story 3.2 — POST /api/orders (Create Order & Start Saga)
**As a buyer**, I want to place an order so the fulfillment process begins.

**Acceptance Criteria:**
- [ ] Validates request: `items` must not be empty, quantities must be > 0
- [ ] Fetches product prices from Inventory Service (`GET /api/products/{id}` REST call) to calculate `totalAmount`
- [ ] Inserts order with `status = PENDING` and all order items (with price snapshot)
- [ ] Publishes `order.created` event **after** transaction commits
- [ ] Returns `201` with `orderId` and `status: "PENDING"`
- [ ] If DB insert fails, event is **not** published

**Service Layer Skeleton:**
```java
@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        // 1. Resolve prices from inventory (REST call or pass from frontend)
        BigDecimal total = calculateTotal(request.items());

        // 2. Persist order
        Order order = new Order(request.userId(), request.shopId(), total);
        request.items().forEach(item -> order.addItem(
            item.productId(), item.productName(), item.unitPrice(), item.quantity()
        ));
        Order saved = orderRepository.save(order);

        // 3. Publish event (after commit — use @TransactionalEventListener or just publish after save)
        OrderCreatedEvent event = new OrderCreatedEvent(
            saved.getId().toString(),
            saved.getUserId(),
            saved.getShopId().toString(),
            mapItems(saved.getItems()),
            saved.getTotalAmount()
        );
        rabbitTemplate.convertAndSend(EventRoutes.EXCHANGE, EventRoutes.ORDER_CREATED, event);

        return new CreateOrderResponse(saved.getId().toString(), "PENDING");
    }
}
```

> ⚠️ **Transactional Gotcha**: Publishing inside `@Transactional` means the event fires *before* the DB commit is visible to other services. Use `@TransactionalEventListener(phase = AFTER_COMMIT)` or publish after the transaction completes to guarantee the order exists in DB before inventory tries to process it.

---

### Story 3.3 — Listen: payment.processed → COMPLETED
**As a system**, when payment succeeds, I want the order status updated to COMPLETED.

**Acceptance Criteria:**
- [ ] `@RabbitListener` on queue `order-service.payment-processed`
- [ ] Deserializes `PaymentProcessedEvent` (Jackson via `Jackson2JsonMessageConverter`)
- [ ] `UPDATE orders SET status='COMPLETED'` for the given `orderId`
- [ ] Publishes `order.completed` event (for Notification Service)
- [ ] Idempotent: if order already `COMPLETED`, log and return without re-publishing

```java
@Component
public class PaymentProcessedListener {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_PROCESSED)
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("Received payment.processed for orderId={}", event.orderId());

        Order order = orderRepository.findById(UUID.fromString(event.orderId()))
            .orElseThrow(() -> new OrderNotFoundException(event.orderId()));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            log.warn("Order {} already COMPLETED, skipping", event.orderId());
            return; // idempotent
        }

        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);

        rabbitTemplate.convertAndSend(EventRoutes.EXCHANGE, EventRoutes.ORDER_COMPLETED,
            new OrderCompletedEvent(event.orderId(), order.getUserId(), order.getTotalAmount()));
    }
}
```

---

### Story 3.4 — Listen: inventory.failed → CANCELLED
**As a system**, when inventory reservation fails, I want the order cancelled immediately.

**Acceptance Criteria:**
- [ ] `@RabbitListener` on queue `order-service.inventory-failed`
- [ ] `UPDATE orders SET status='CANCELLED'`
- [ ] Publishes `order.cancelled` event
- [ ] Idempotent: if already `CANCELLED`, skip

---

### Story 3.5 — Listen: inventory.released → CANCELLED
**As a system**, when payment fails and inventory is released, I want the order marked as CANCELLED.

**Acceptance Criteria:**
- [ ] `@RabbitListener` on queue `order-service.inventory-released`
- [ ] Same cancellation logic as Story 3.4
- [ ] Idempotent handling

---

### Story 3.6 — GET /api/orders/{orderId}
**As a buyer**, I want to check my order status so the frontend can poll for the result.

**Acceptance Criteria:**
- [ ] Returns full order DTO including `status`, `totalAmount`, `items`
- [ ] Returns `404` with error body if `orderId` not found
- [ ] Frontend polls this every 2 seconds until status != `PENDING`

---

## ✅ Epic 3 Definition of Done

- [ ] `POST /api/orders` creates a PENDING order and publishes `order.created`
- [ ] Order → COMPLETED when `payment.processed` received
- [ ] Order → CANCELLED when `inventory.failed` received
- [ ] Order → CANCELLED when `inventory.released` received
- [ ] `GET /api/orders/{orderId}` returns current status
- [ ] All listeners are idempotent
- [ ] Actuator health endpoint returns `UP`
