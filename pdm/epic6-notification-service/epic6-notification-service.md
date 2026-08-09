# Epic 6 — Notification Service (Observer Pattern)

## Overview

The Notification Service proves the power of choreography with one elegant demonstration:

> You can add a brand new feature (email notifications) **without modifying a single line of code** in any existing service.

Order Service, Inventory Service, and Payment Service don't know this service exists. The Notification Service silently subscribes to `order.*` events and logs mock emails.

---

## 📐 Saga Role (Observer — Outside the Saga)

```
                  ORDER SERVICE
                      │
              order.completed
              order.cancelled
                      │
                      ▼ (via RabbitMQ topic exchange — wildcard)
        ┌──────────────────────────────────┐
        │   NOTIFICATION SERVICE           │
        │   (no HTTP port — consumer only) │
        │                                  │
        │  Subscribes to: order.*          │
        │                                  │
        │  order.completed →               │
        │    log("📧 Email: Order ready!") │
        │                                  │
        │  order.cancelled →               │
        │    log("📧 Email: Order cancelled")│
        └──────────────────────────────────┘
```

The wildcard `order.*` binding means: if you later add `order.delayed` or `order.refunded`, the Notification Service automatically receives those too — without anyone touching the publisher.

---

## 🖼️ Console Output Example

```
[notification-service] 📧 Mock Email Sent
  To:      user-john@example.com
  Subject: Your TechNest order is COMPLETE! 🎉
  Body:    Hi John! Your order #550e8400-... for $59.98 has been
           processed. Your items are on their way!

[notification-service] 📧 Mock Email Sent
  To:      user-jane@example.com
  Subject: Your FreshWear order has been cancelled
  Body:    Hi Jane! Your order #c1a2b3c4-... could not be completed.
           Reason: Insufficient funds (simulated). No charges were made.
```

---

## 📋 Stories

### Story 6.1 — Spring Boot Application (No HTTP Server)
**As a developer**, I want a Spring Boot app that runs purely as a message consumer with no web server.

**Acceptance Criteria:**
- [ ] Service has `spring-boot-starter-amqp` but **NOT** `spring-boot-starter-web`
- [ ] `application.yml` sets `spring.main.web-application-type: none` to disable HTTP server
- [ ] Connects to RabbitMQ on startup
- [ ] Declares queue `notification-service.orders` bound to exchange `saga.events` with routing key `order.*`
- [ ] Logs on startup: `🔔 Notification Service: listening for order events...`

**application.yml:**
```yaml
spring:
  main:
    web-application-type: none   # ← No HTTP server!
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: 5672
    username: ${RABBITMQ_USER}
    password: ${RABBITMQ_PASS}
```

**RabbitMQ Config:**
```java
@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_ORDERS = "notification-service.orders";

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(EventRoutes.EXCHANGE, true, false);
    }

    @Bean
    public Queue ordersQueue() {
        return QueueBuilder.durable(QUEUE_ORDERS).build();
    }

    @Bean
    public Binding ordersBinding(Queue ordersQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(ordersQueue)
                .to(sagaExchange)
                .with("order.*");   // ← wildcard binding!
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

---

### Story 6.2 — Handle order.completed → Success Notification
**As a system**, when an order is completed, I want to log a mock success email.

**Acceptance Criteria:**
- [ ] Detects routing key `order.completed` from the `Message` header or payload
- [ ] Logs formatted success email with: userId, orderId, totalAmount
- [ ] Uses `@RabbitListener` with `Message` parameter to access routing key

```java
@Component
@Slf4j
public class OrderEventListener {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDERS)
    public void handleOrderEvent(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        switch (routingKey) {
            case EventRoutes.ORDER_COMPLETED -> handleCompleted(body);
            case EventRoutes.ORDER_CANCELLED -> handleCancelled(body);
            default -> log.debug("Unhandled order event: {}", routingKey);
        }
    }

    private void handleCompleted(String body) {
        // Deserialize and log mock success email
        OrderCompletedEvent event = deserialize(body, OrderCompletedEvent.class);
        log.info("""
            📧 Mock Email Sent
              To:      {}@example.com
              Subject: Your order is COMPLETE! 🎉
              Body:    Your order #{} for ${} has been processed successfully!
            """,
            event.userId(), event.orderId(), event.totalAmount()
        );
    }

    private void handleCancelled(String body) {
        OrderCancelledEvent event = deserialize(body, OrderCancelledEvent.class);
        log.info("""
            📧 Mock Email Sent
              To:      {}@example.com
              Subject: Your order has been cancelled
              Body:    Order #{} was cancelled. Reason: {}. No charges were made.
            """,
            event.userId(), event.orderId(), event.reason()
        );
    }
}
```

---

### Story 6.3 — Distinguish Cancellation Reasons
**As a system**, I want cancellation emails to reflect whether the cause was out-of-stock or payment failure.

**Acceptance Criteria:**
- [ ] `OrderCancelledEvent` includes a `reason` field (set by Order Service when publishing)
- [ ] Notification logs distinct messages:
  - `"Item out of stock"` → "We're sorry — this item is no longer available"
  - `"Insufficient funds (simulated)"` → "Your payment could not be processed"

---

## ✅ Epic 6 Definition of Done

- [ ] Notification Service starts with no HTTP port (`web-application-type: none`)
- [ ] Subscribes to `order.*` (wildcard routing key)
- [ ] Logs success email when `order.completed` is received
- [ ] Logs cancellation email when `order.cancelled` is received
- [ ] Adding this service required **zero changes** to any other service
