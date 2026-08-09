# Epic 1 — Foundation & Infrastructure

## Overview

This is the most critical epic. You're building the **skeleton that all other services depend on**. Get this right and every subsequent epic becomes much easier. Rushing this is the #1 mistake new microservice engineers make.

By the end of this epic you will have:
- A monorepo where all Spring Boot services live side by side
- A single `docker compose up` command that starts RabbitMQ + PostgreSQL
- A shared Java library (`shared-events`) so all services speak the same "language"
- A working Spring AMQP publisher/listener pattern you can copy into every service

---

## 🧭 Why Choreography? (Background Reading)

There are two ways to coordinate a Saga:

| | Orchestration | **Choreography** (what we're building) |
|-|---|---|
| **How** | One "brain" service tells others what to do | Each service reacts to events independently |
| **Coupling** | High — all services know about the orchestrator | Low — services only know about events |
| **Failure** | Single point of failure | More resilient |
| **Complexity** | Business logic centralized (easier to read) | Distributed (harder to trace) |

Choreography is harder to debug but scales better and forces you to think event-first.

---

## 📐 Architecture Diagram

```
ecommerces/ (monorepo root)
│
├── services/
│   ├── order-service/            (Spring Boot, port 8081)
│   ├── inventory-service/        (Spring Boot, port 8082)
│   ├── payment-service/          (Spring Boot, port 8083)
│   └── notification-service/     (Spring Boot, no HTTP port)
│
├── shared/
│   └── events/
│       └── src/main/java/.../EventRoutes.java   ← routing key constants
│
├── docker-compose.yml     ← Starts: RabbitMQ, PostgreSQL (x3 DBs)
└── .env (or compose env_file)
```

### RabbitMQ Exchange Design

```
Exchange: "saga.events"  (type: topic)
│
├── Routing Key: "order.created"       → consumed by: inventory-service
├── Routing Key: "inventory.reserved"  → consumed by: payment-service
├── Routing Key: "inventory.failed"    → consumed by: order-service
├── Routing Key: "payment.processed"   → consumed by: order-service
├── Routing Key: "payment.failed"      → consumed by: inventory-service
├── Routing Key: "inventory.released"  → consumed by: order-service
├── Routing Key: "order.completed"     → consumed by: notification-service
└── Routing Key: "order.cancelled"     → consumed by: notification-service
```

> **Why topic exchange?** A topic exchange routes messages by pattern (e.g., `order.*` matches all order events). This lets the Notification Service subscribe to `order.*` without caring about individual event names — perfectly demonstrating the Observer pattern.

---

## 🖼️ Screen Flow

*This epic has no UI — it's pure infrastructure.*

---

## 📋 Stories

### Story 1.1 — Initialize Monorepo Structure
**As a developer**, I want a clean folder structure so that all services are organized consistently.

**Acceptance Criteria:**
- [x] Root directory contains `services/`, `shared/`, `scripts/`, `docker-compose.yml`
- [x] Each service is a standalone Maven project with its own `pom.xml`
- [x] `shared/events/` is a Maven module with its own `pom.xml` (packaged as a JAR)
- [x] Each service's `pom.xml` depends on `shared-events` as a local Maven dependency
- [ ] A root-level `README.md` explains how to start the project
- [x] `.gitignore` covers `target/`, `.env`, `*.class`, IDE files

**Suggested Maven Coordinates:**
```xml
<!-- shared/events/pom.xml -->
<groupId>com.ecommerces</groupId>
<artifactId>shared-events</artifactId>
<version>1.0.0</version>
<packaging>jar</packaging>

<!-- services/order-service/pom.xml -->
<dependency>
    <groupId>com.ecommerces</groupId>
    <artifactId>shared-events</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Key Spring Boot Dependencies (all services):**
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
</dependencies>
```

---

### Story 1.2 — Docker Compose: RabbitMQ + PostgreSQL
**As a developer**, I want to start all infrastructure with one command so I don't manually install anything.

**Acceptance Criteria:**
- [x] `docker-compose.yml` defines services: `rabbitmq`, `postgres`
- [x] RabbitMQ has the management UI enabled (port 15672) so you can visually inspect queues
- [x] PostgreSQL has **3 separate databases**: `orders_db`, `inventory_db`, `payments_db`
  - Each service owns its own DB — this is a core microservice principle
- [x] Health checks defined so Spring Boot services wait for infra to be ready
- [x] All credentials stored in `.env` file and referenced via `env_file` in compose

**docker-compose.yml skeleton:**
```yaml
version: '3.8'
services:
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"       # AMQP protocol
      - "15672:15672"     # Management UI
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASS}
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 10s
      retries: 5

  postgres:
    image: postgres:15
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASS}
    volumes:
      - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER}"]
      interval: 10s
      retries: 5
```

```sql
-- scripts/init-db.sql
CREATE DATABASE orders_db;
CREATE DATABASE inventory_db;
CREATE DATABASE payments_db;
```

**Each service's `application.yml`:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orders_db
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASS}
  rabbitmq:
    host: localhost
    port: 5672
    username: ${RABBITMQ_USER}
    password: ${RABBITMQ_PASS}
  flyway:
    enabled: true
```

---

### Story 1.3 — Shared Event Constants (Java Library)
**As a developer**, I want all RabbitMQ routing keys defined in one place so there's never a typo mismatch between publisher and consumer.

**Acceptance Criteria:**
- [x] `shared/events/src/main/java/com/ecommerces/events/EventRoutes.java` — constants class
- [ ] `shared/events/src/main/java/com/ecommerces/events/dto/` — one DTO record per event payload
- [x] All services declare this as a Maven dependency
- [x] Install to local Maven repo with `mvn install` from `shared/events/`

**Implementation:**
```java
// EventRoutes.java
public final class EventRoutes {
    public static final String EXCHANGE       = "saga.events";

    public static final String ORDER_CREATED        = "order.created";
    public static final String INVENTORY_RESERVED   = "inventory.reserved";
    public static final String INVENTORY_FAILED     = "inventory.failed";
    public static final String INVENTORY_RELEASED   = "inventory.released";
    public static final String PAYMENT_PROCESSED    = "payment.processed";
    public static final String PAYMENT_FAILED       = "payment.failed";
    public static final String ORDER_COMPLETED      = "order.completed";
    public static final String ORDER_CANCELLED      = "order.cancelled";

    private EventRoutes() {}
}
```

```java
// dto/OrderCreatedEvent.java
public record OrderCreatedEvent(
    String orderId,
    String userId,
    String shopId,
    List<OrderItem> items,
    BigDecimal totalAmount
) {}

// dto/OrderItem.java
public record OrderItem(String productId, int quantity) {}

// dto/InventoryReservedEvent.java
public record InventoryReservedEvent(String orderId, List<OrderItem> items) {}

// dto/InventoryFailedEvent.java
public record InventoryFailedEvent(String orderId, String reason) {}

// dto/PaymentProcessedEvent.java
public record PaymentProcessedEvent(String orderId, BigDecimal amount, String transactionId) {}

// dto/PaymentFailedEvent.java
public record PaymentFailedEvent(String orderId, String reason) {}

// dto/InventoryReleasedEvent.java
public record InventoryReleasedEvent(String orderId) {}

// dto/OrderCompletedEvent.java
public record OrderCompletedEvent(String orderId, String userId, BigDecimal totalAmount) {}

// dto/OrderCancelledEvent.java
public record OrderCancelledEvent(String orderId, String userId, String reason) {}
```

---

### Story 1.4 — RabbitMQ Configuration Bean
**As a developer**, I want a reusable Spring AMQP configuration so every service declares the same exchange consistently.

**Acceptance Criteria:**
- [x] Each service has a `RabbitMQConfig.java` `@Configuration` class
- [x] Declares a `TopicExchange` bean named `saga.events`
- [x] Declares its service-specific durable queues and bindings
- [x] Uses `JacksonJsonMessageConverter` so events are serialized as JSON automatically
- [x] `RabbitTemplate` is configured with the JSON converter (for publishing)

**Implementation:**
```java
// config/RabbitMQConfig.java  (example for order-service)
@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_PAYMENT_PROCESSED = "order-service.payment-processed";
    public static final String QUEUE_INVENTORY_FAILED  = "order-service.inventory-failed";
    public static final String QUEUE_INVENTORY_RELEASED= "order-service.inventory-released";

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(EventRoutes.EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentProcessedQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_PROCESSED).build();
    }

    @Bean
    public Binding paymentProcessedBinding(Queue paymentProcessedQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(paymentProcessedQueue)
                .to(sagaExchange)
                .with(EventRoutes.PAYMENT_PROCESSED);
    }

    // ... repeat for other queues

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
```

> ⚠️ **Common Gotcha**: Always configure the `MessageConverter` on **both** `RabbitTemplate` (publisher side) and the `SimpleRabbitListenerContainerFactory` (consumer side). If they don't match, deserialization will fail.

---

### Story 1.5 — Verify End-to-End Event Flow (Smoke Test)
**As a developer**, I want to confirm messages flow from publisher to consumer before building real business logic.

**Acceptance Criteria:**
- [ ] A temporary `SmokeTestController` in order-service publishes an `order.created` event via `RabbitTemplate`
- [ ] A temporary `@RabbitListener` in inventory-service receives and logs it
- [ ] RabbitMQ Management UI (localhost:15672) shows the queue and message
- [ ] Message is received as a deserialized Java object (not raw bytes)
- [ ] Remove the smoke test code before proceeding to Epic 3

---

## ✅ Epic 1 Definition of Done

- [x] `docker compose up` starts RabbitMQ and PostgreSQL with no errors
- [x] RabbitMQ Management UI accessible at http://localhost:15672
- [x] All 3 databases exist and are accessible
- [ ] `shared-events` JAR installs with `mvn install` and is importable in all services
- [ ] Smoke test proves publish → consume works with JSON deserialization
- [ ] All credentials are in `.env` (not committed to git)
