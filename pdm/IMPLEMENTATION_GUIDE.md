# 🗺️ Implementation Guide — Phased Approach

> **For someone new to microservices**: Read this before touching any code.  
> Each phase builds on the last. Don't skip phases — the concepts stack.

---

## Tech Stack Quick Reference

| What | Tool |
|------|------|
| Services | Java 21 + Spring Boot 3 |
| Build | Maven (`./mvnw`) |
| Messaging | RabbitMQ via Spring AMQP |
| Database | PostgreSQL via Spring Data JPA |
| Migrations | Flyway (auto-runs on startup) |
| Frontend | React Router v7 (framework mode) + Vite |
| Infra | Docker Compose |

---

## How to Use This Guide

1. **Read** the phase overview before starting
2. **Open** the linked epic document for detailed stories and code skeletons
3. **Implement** story by story — don't jump ahead
4. **Verify** the phase's Definition of Done before proceeding
5. **Commit** your work at the end of each epic

---

## Phase 1 — Foundation
**Epic**: [Epic 1 — Infrastructure](./epic1-foundation/epic1-foundation.md)  
**Estimated Time**: 1–2 days  
**Goal**: "I can publish a message from one Spring Boot service and receive it in another"

### What You're Learning
- How RabbitMQ topic exchanges and bindings work
- Spring AMQP: `RabbitTemplate` (publish) and `@RabbitListener` (consume)
- Why each service needs its own database (bounded context)
- Sharing code across services via a local Maven JAR

### Step-by-Step Checklist
- [ ] Read Epic 1 fully before writing code
- [ ] Create the folder structure: `services/`, `shared/`, `scripts/`
- [ ] Write `docker-compose.yml` with RabbitMQ + PostgreSQL
- [ ] Run `docker compose up` — verify RabbitMQ UI at http://localhost:15672
- [ ] Build `shared/events/` Maven module — run `mvn install`
- [ ] Create first Spring Boot service (order-service) with AMQP dependency
- [ ] Write `RabbitMQConfig.java` — declare exchange + queue
- [ ] Smoke test: publish `order.created` from a temp endpoint, receive it with `@RabbitListener` in inventory-service
- [ ] Verify message appears in RabbitMQ Management UI
- [ ] ✅ Phase 1 complete when: publish → consume works with JSON deserialization

### Common Mistakes at This Phase
- **Hardcoding credentials** — always read from `${ENV_VAR}` in `application.yml`
- **Not declaring the exchange in both services** — both publisher and consumer must `assertExchange`; in Spring, declare the `TopicExchange` bean in both services' configs
- **Missing `Jackson2JsonMessageConverter`** — without this, messages are sent as raw bytes and deserialization fails
- **Forgetting to `ack`** — Spring AMQP auto-acks on successful handler return; throws on exception. If your handler throws, the message is requeued — make sure handlers don't throw on business errors (publish a failure event instead)

---

## Phase 2 — Data Layer
**Epic**: [Epic 2 — Seed Data](./epic2-seed-data/epic2-seed-data.md)  
**Estimated Time**: 1 day  
**Goal**: "I can query products and stock levels from a JPA repository"

### What You're Learning
- Spring Data JPA: `@Entity`, `@Repository`, relationships
- `@Version` for optimistic locking (understand this before Phase 3!)
- Flyway: version-controlled SQL migrations that auto-run on startup
- Database-per-service principle

### Step-by-Step Checklist
- [ ] Read Epic 2 fully — especially the `@Version` explanation
- [ ] Write Flyway migration SQL for all 3 services (`src/main/resources/db/migration/`)
- [ ] Start services — Flyway runs migrations automatically on startup
- [ ] Write JPA entity classes with correct annotations
- [ ] Write `JpaRepository` interfaces for each entity
- [ ] Add seed data via Flyway `V5__seed_data.sql` in inventory-service
- [ ] Test: `curl http://localhost:8082/api/shops`
- [ ] ✅ Phase 2 complete when: products API returns data with correct stock levels

### Common Mistakes at This Phase
- **Forgetting `@Version` on Stock entity** — you CANNOT add it later without a migration
- **Exposing JPA entities directly in REST responses** — always use DTOs/Records
- **Sharing databases between services** — Order Service must NOT query `inventory_db`
- **Flyway naming** — must be `V{number}__{description}.sql` (double underscore)

---

## Phase 3 — Core Saga (The Hard Part)
**Epics**:  
- [Epic 3 — Order Service](./epic3-order-service/epic3-order-service.md)  
- [Epic 4 — Inventory Service](./epic4-inventory-service/epic4-inventory-service.md)  
- [Epic 5 — Payment Service](./epic5-payment-service/epic5-payment-service.md)  
**Estimated Time**: 3–5 days  
**Goal**: "A complete order flows through all 3 services and reaches COMPLETED or CANCELLED"

### Build in This Exact Order

#### Step 1: Order Service REST endpoint (Epic 3, Story 3.2)
Build the API endpoint first. Don't add event listeners yet.
- [ ] `POST /api/orders` → inserts PENDING order + publishes `order.created`
- [ ] Test with curl:
  ```bash
  curl -X POST http://localhost:8081/api/orders \
    -H "Content-Type: application/json" \
    -d '{"userId":"test-user","shopId":"a1b2c3d4-0001-0001-0001-000000000001","items":[{"productId":"prod-0001-0001-0001-000000000001","quantity":1}]}'
  ```
- [ ] Verify `order.created` appears in RabbitMQ Management UI

#### Step 2: Inventory Service stock reservation (Epic 4, Story 4.2)
- [ ] `@RabbitListener` on `order.created` queue
- [ ] Implement stock deduction — first test with a simple UPDATE, then add `@Version`
- [ ] Test: place an order, watch inventory logs, check stock reduced in DB

#### Step 3: Close the first loop
- [ ] Inventory publishes `inventory.reserved` on success
- [ ] Inventory publishes `inventory.failed` on out-of-stock
- [ ] Order Service listens to `inventory.failed` → sets status CANCELLED
- [ ] Test: order a Laptop Stand (stock=0) → should be CANCELLED

#### Step 4: Payment Service (Epic 5)
- [ ] `@RabbitListener` on `inventory.reserved`
- [ ] Set `PAYMENT_FAILURE_RATE=0.0` first — force all successes
- [ ] Payment publishes `payment.processed`
- [ ] Order Service listens → COMPLETED
- [ ] Test full happy path via curl + poll `GET /api/orders/:id`

#### Step 5: Payment failure rollback
- [ ] Set `PAYMENT_FAILURE_RATE=1.0`
- [ ] Payment publishes `payment.failed`
- [ ] Inventory listens → restores stock → publishes `inventory.released`
- [ ] Order Service listens → CANCELLED
- [ ] Verify stock restored in DB

#### Step 6: Idempotency
- [ ] Manually republish an event in RabbitMQ UI for an already-processed order
- [ ] Verify: handlers skip gracefully (log a warning, don't double-process)

### Common Mistakes at This Phase
- **Publishing inside `@Transactional` before commit** — the DB row might not be committed when the consuming service reads it. Use `@TransactionalEventListener(phase = AFTER_COMMIT)` for publishing
- **Throwing exceptions in `@RabbitListener`** — Spring will redeliver the message. Catch business exceptions and publish failure events instead
- **Race condition in compensation** — the `Reservation` table is your safety net; always check it exists before restoring stock
- **Missing `@Version` on Stock entity** — without it, optimistic locking doesn't work (concurrent updates will not fail)

---

## Phase 4 — Observer
**Epic**: [Epic 6 — Notification Service](./epic6-notification-service/epic6-notification-service.md)  
**Estimated Time**: 0.5 days  
**Goal**: "I add a new service without touching existing code"

### Step-by-Step Checklist
- [ ] Create `services/notification-service/` — Spring Boot with AMQP only, no web
- [ ] Set `spring.main.web-application-type: none` in `application.yml`
- [ ] Declare queue bound to `order.*` wildcard
- [ ] Handle `order.completed` → log success email
- [ ] Handle `order.cancelled` → log cancellation email
- [ ] Start the service alongside the others — observe logs without changing any other service
- [ ] ✅ Appreciate this moment — you just added a feature to a distributed system with zero coupling

---

## Phase 5 — Frontend
**Epic**: [Epic 7 — Buyer UI](./epic7-frontend/epic7-frontend.md)  
**Estimated Time**: 3–4 days  
**Goal**: "A real person can place an order through a browser and see the result"

### React Router v7 Key Concepts
- **`loader`** — runs server-side (or client-side in SPA mode) before the component renders. Use for data fetching.
- **`action`** — handles form submissions. Use for `POST /api/orders`.
- **File-based routing** — `app/routes/shops.$shopId.tsx` maps to `/shops/:shopId`

### Build in This Order
1. Project setup + global CSS design system
2. Home page with shop cards (loader + JSX)
3. Product listing (loader + cart "add" button)
4. Cart context + localStorage persistence
5. Checkout page (action → calls Order API → redirect)
6. Order status page (polling `useEffect`)

### Common Mistakes at This Phase
- **CORS errors** — configure `@CrossOrigin` or a global `WebMvcConfigurer` bean on every Spring Boot service; the frontend at `localhost:5173` will be blocked otherwise
- **Polling without cleanup** — always `clearTimeout` in the `useEffect` cleanup to avoid memory leaks
- **Trusting client prices** — the Order Service should resolve prices from the Inventory Service, not trust what the frontend sends

---

---

## Phase 6 — Service Discovery & API Gateway
**Epics**:  
- [Epic 8 — Service Registry](./epic8-service-registry/epic8-service-registry.md)  
- [Epic 9 — API Gateway](./epic9-api-gateway/epic9-api-gateway.md)  
**Estimated Time**: 2 days  
**Goal**: "Frontend talks only to the API Gateway on port 8080, and backend services dynamically discover each other via Eureka"

### What You're Learning
- Service registration and dynamic host discovery with Spring Cloud Netflix Eureka
- Client-side load balancing via `lb://` URI schemes
- Centralizing cross-cutting concerns (CORS, request routing, correlation IDs) in Spring Cloud Gateway
- Implementing Authentication (AuthN) with AWS Cognito as an OAuth2 Resource Server at the Gateway
- Implementing coarse-grained Authorization (AuthZ) at the Gateway (route RBAC) and context propagation (`X-User-Id`, `X-User-Roles`) to downstream services for fine-grained domain AuthZ
- Decoupling frontend network knowledge from internal microservice topologies

### Step-by-Step Checklist
- [ ] Create `services/discovery-service/` with `spring-cloud-starter-netflix-eureka-server`
- [ ] Annotate with `@EnableEurekaServer` and configure standalone mode on port `8761`
- [ ] Add `spring-cloud-starter-netflix-eureka-client` to all backend services (`order-service`, `inventory-service`, `payment-service`, `notification-service`)
- [ ] Verify all services appear under `http://localhost:8761` dashboard
- [ ] Create `services/api-gateway/` with `spring-cloud-starter-gateway`, `spring-boot-starter-security`, and `spring-boot-starter-oauth2-resource-server` on port `8080`
- [ ] Configure dynamic routes (`/api/orders/**` -> `lb://order-service`, `/api/shops/**` -> `lb://inventory-service`)
- [ ] Configure `SecurityWebFilterChain` to validate Cognito JWTs (JWKS) and define public vs protected routes
- [ ] Implement `UserContextFilter` to extract Cognito claims (`sub`, `email`, `cognito:groups`) and inject `X-User-Id`, `X-User-Email`, `X-User-Roles` headers
- [ ] Add global CORS rules in Gateway `application.yml` for frontend origins
- [ ] Add `CorrelationTrackingFilter` to attach `X-Correlation-Id`
- [ ] Update frontend environment configuration to point to `http://localhost:8080` and attach Cognito Bearer token
- [ ] Test end-to-end checkout flow purely through the Gateway port `8080`

### Common Mistakes at This Phase
- **Accidentally including `spring-boot-starter-web` (Tomcat) in `api-gateway`** — Spring Cloud Gateway is reactive (Netty/WebFlux) and will crash if Tomcat is on the classpath
- **Missing `spring.application.name`** — Eureka uses this property to name the registered service
- **Header Spoofing Vulnerability** — Not sanitizing/stripping incoming `X-User-*` headers from untrusted clients before forwarding requests downstream
- **Redundant JWT Validation Downstream** — Having downstream services re-validate Cognito JWTs rather than consuming the trusted internal user headers provided by the Gateway boundary
- **Container IP resolution issues in Docker Compose** — configure `eureka.instance.prefer-ip-address: true` so containers discover each other via IP/Docker DNS

---

## Phase 7 — Zero-Trust Security & End-to-End JWT Propagation
**Epic**: [Epic 10 — End-to-End JWT Propagation & Zero-Trust Security](./epic10-jwt-propagation/epic10-jwt-propagation.md)  
**Estimated Time**: 1–2 days  
**Goal**: "Replace unverified plain X-User-* headers with cryptographically verified Bearer JWT tokens propagated across all microservices"

### What You're Learning
- Transitioning from Perimeter Security to Zero-Trust Architecture (Defense-in-Depth)
- Token Relay filter patterns in Spring Cloud Gateway
- Configuring Spring Boot microservices as independent OAuth2 Resource Servers
- Validating AWS Cognito JWKS signatures and token expiration within each service
- Extracting authenticated user context (`sub`, `email`, `roles`) via native Spring Security contexts (`@AuthenticationPrincipal Jwt`, `SecurityContextHolder`)
- Eliminating internal header spoofing risks

### Step-by-Step Checklist
- [ ] Add `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server` to `order-service`, `inventory-service`, and `payment-service`
- [ ] Configure `SecurityConfig` in each microservice to validate Cognito JWKS
- [ ] Configure `jwtAuthenticationConverter` in each service to map `cognito:groups` to `ROLE_*` authorities
- [ ] Update controllers to inject `@AuthenticationPrincipal Jwt` and extract verified `sub` / `email`
- [ ] Ensure API Gateway preserves and relays the `Authorization: Bearer <JWT>` header
- [ ] Verify that direct unauthenticated or spoofed requests to downstream services are rejected with `401 Unauthorized`
- [ ] Test complete end-to-end checkout with valid tokens

---

## Phase 8 — Quality & Observability
**Epic**: [Epic 11 — Race Condition Testing & Observability](./epic11-testing-observability/epic11-testing-observability.md)  
**Estimated Time**: 1–2 days  
**Goal**: "I can prove my system is correct under concurrent load through the API Gateway"

### Step-by-Step Checklist
- [ ] Install `axios` in `scripts/`: `cd scripts && npm init -y && npm install axios`
- [ ] Run `node scripts/race-condition-test.js` against Gateway (`http://localhost:8080`) — it will likely FAIL first if you didn't implement `@Version`
- [ ] Fix concurrency issues until test PASSES consistently (run 3 times to be sure)
- [ ] Add `logstash-logback-encoder` + `logback-spring.xml` to all services
- [ ] Add `MDC.put("orderId", ...)` to all `@RabbitListener` handlers
- [ ] Run the full flow and trace one saga: `docker compose logs | grep "<some-orderId>"`
- [ ] Verify `/actuator/health` returns correct status on all services and Gateway

---

## 🎯 Milestones Summary

| Milestone | What It Proves |
|-----------|---------------|
| Phase 1 Done | You understand Spring AMQP + RabbitMQ topic routing |
| Phase 2 Done | You can model a domain with JPA and manage schema with Flyway |
| Phase 3 Done | You can implement a distributed saga with compensation |
| Phase 4 Done | You understand the open/closed principle at system scale |
| Phase 5 Done | End-to-end working product with real UI |
| Phase 6 Done | Dynamic service discovery and unified API Gateway routing |
| Phase 7 Done | Zero-trust microservice security with end-to-end JWT token propagation |
| Phase 8 Done | Your system is provably correct under concurrent load |

---

## 💡 Tips for Learning

1. **One terminal per service** — watching all logs side-by-side is invaluable
2. **Use RabbitMQ Management UI** — publish test messages directly to queues to debug consumers without needing to trigger the full flow
3. **Break something intentionally** — remove `@Version` from Stock, run the race condition test, watch it fail. Add it back. Understanding the bug makes the solution memorable.
4. **Commit after each story** — your git history becomes your progress tracker
5. **Read Spring AMQP docs** on `@RabbitListener`, `RabbitTemplate`, and error handling — these are the core of the entire saga

---

## 📚 Concepts to Research Before Each Phase

| Before Phase | Research These |
|---|---|
| 1 | RabbitMQ topic exchanges, Spring AMQP, `Jackson2JsonMessageConverter` |
| 2 | JPA `@Version` (optimistic locking), Flyway migrations, Spring Data JPA |
| 3 | Saga pattern, compensation transactions, `@TransactionalEventListener`, idempotency |
| 4 | Observer pattern, `spring.main.web-application-type: none` |
| 5 | React Router v7 loaders/actions, React Context, `useEffect` cleanup |
| 6 | Service discovery with Eureka, reactive routing with Spring Cloud Gateway, client load balancing |
| 7 | Zero-Trust architecture, OAuth2 Resource Server, JWT signature validation, Token Relay |
| 8 | MDC (Mapped Diagnostic Context), Logback JSON encoding, concurrent HTTP clients |
