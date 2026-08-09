# Epic 8 — Race Condition Testing & Observability

## Overview

This epic turns your working system into a **provably correct** system. You'll write a concurrent load test that intentionally breaks naive implementations, and add structured logging so you can trace every event through the entire saga.

This is what separates a portfolio project from a production system.

---

## 📐 Race Condition Test Design

```
scripts/race-condition-test.js
│
│  USB-C Hub: quantity = 3
│
│  Fire 10 simultaneous POST /api/orders
│  Each requesting qty = 1
│  ┌──────────────────────────────────────┐
│  │ Promise.all([                        │
│  │   order1, order2, ... order10        │  ← all fire simultaneously
│  │ ])                                   │
│  └──────────────────────────────────────┘
│
│  Expected outcome (with correct @Version locking):
│  ✅ 3 orders → COMPLETED
│  ❌ 7 orders → CANCELLED
│
│  Any other result = concurrency bug!
```

The test script is Node.js (not Java) — it's a simple external HTTP client,
not part of the application logic. No need to spin up a Spring Boot test context.

---

## 📊 Observability: Structured Logging with SLF4J + Logback

Every service should emit **structured logs** with the `orderId` as a correlation key. Configure Logback to output JSON so logs can be parsed and grepped.

**Add logstash-logback-encoder dependency:**
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

**logback-spring.xml (in each service's resources/):**
```xml
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"order-service"}</customFields>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

**Logging in service code:**
```java
// Use MDC for the correlation ID so it appears on every log line
MDC.put("orderId", event.orderId());
log.info("Order created, publishing saga start event");
// ...
MDC.clear();
```

**Example JSON log output:**
```json
{"@timestamp":"2026-01-01T10:00:00Z","level":"INFO","service":"order-service","message":"Order created","orderId":"ord-abc","userId":"user-john"}
{"@timestamp":"2026-01-01T10:00:01Z","level":"INFO","service":"inventory-service","message":"Stock reservation attempt","orderId":"ord-abc","productId":"usb-hub","requestedQty":1}
{"@timestamp":"2026-01-01T10:00:01Z","level":"INFO","service":"inventory-service","message":"Stock reserved successfully","orderId":"ord-abc"}
{"@timestamp":"2026-01-01T10:00:02Z","level":"INFO","service":"payment-service","message":"Processing payment","orderId":"ord-abc","amount":29.99}
{"@timestamp":"2026-01-01T10:00:03Z","level":"INFO","service":"payment-service","message":"Payment succeeded","orderId":"ord-abc","transactionId":"txn_xxx"}
{"@timestamp":"2026-01-01T10:00:03Z","level":"INFO","service":"order-service","message":"Order completed","orderId":"ord-abc"}
{"@timestamp":"2026-01-01T10:00:03Z","level":"INFO","service":"notification-service","message":"Sent success email","orderId":"ord-abc"}
```

Trace a complete saga: `docker compose logs | grep "ord-abc"`

---

## 📋 Stories

### Story 8.1 — Race Condition Load Test Script
**As a developer**, I want a script that fires concurrent orders to prove `@Version` locking works.

**Acceptance Criteria:**
- [ ] `scripts/race-condition-test.js` sends 10 simultaneous POST requests
- [ ] All target USB-C Hub (stock=3), qty=1 each
- [ ] Uses `Promise.all()` to fire simultaneously
- [ ] Polls each `orderId` every 1 second until status != PENDING
- [ ] Prints summary report:
  - Number of COMPLETED / CANCELLED / TIMEOUT
  - **PASS** if `COMPLETED === 3` and `CANCELLED === 7`
  - **FAIL** otherwise (concurrency bug)

```javascript
// scripts/race-condition-test.js
const axios = require('axios');

const ORDER_API = 'http://localhost:8081';
const USB_HUB_ID  = process.env.USB_HUB_PRODUCT_ID  || 'prod-0001-0001-0001-000000000001';
const SHOP_ID     = process.env.TECHNEST_SHOP_ID     || 'a1b2c3d4-0001-0001-0001-000000000001';
const N = 10;

const sleep = ms => new Promise(r => setTimeout(r, ms));

async function placeOrder(userId) {
  const { data } = await axios.post(`${ORDER_API}/api/orders`, {
    userId,
    shopId: SHOP_ID,
    items: [{ productId: USB_HUB_ID, quantity: 1 }],
  });
  return data.orderId;
}

async function pollStatus(orderId, max = 30) {
  for (let i = 0; i < max; i++) {
    await sleep(1000);
    const { data } = await axios.get(`${ORDER_API}/api/orders/${orderId}`);
    if (data.status !== 'PENDING') return data.status;
  }
  return 'TIMEOUT';
}

async function run() {
  console.log(`🧪 Firing ${N} simultaneous orders for USB-C Hub (stock=3)...\n`);

  const orderIds = await Promise.all(
    Array.from({ length: N }, (_, i) => placeOrder(`race-user-${i + 1}`))
  );

  console.log(`✅ All ${N} orders placed. Polling for results...\n`);
  const statuses = await Promise.all(orderIds.map(id => pollStatus(id)));

  const completed = statuses.filter(s => s === 'COMPLETED').length;
  const cancelled = statuses.filter(s => s === 'CANCELLED').length;
  const timeout   = statuses.filter(s => s === 'TIMEOUT').length;

  console.log('─'.repeat(40));
  console.log(`📊 RESULTS:`);
  console.log(`  ✅ COMPLETED : ${completed}`);
  console.log(`  ❌ CANCELLED : ${cancelled}`);
  console.log(`  ⏰ TIMEOUT   : ${timeout}`);
  console.log('─'.repeat(40));

  const pass = completed === 3 && cancelled === 7 && timeout === 0;
  console.log(`\n${pass ? '🟢 PASS' : '🔴 FAIL'}`);
  if (!pass) {
    console.log('  Expected: 3 COMPLETED, 7 CANCELLED, 0 TIMEOUT');
    console.log('  Check @Version optimistic locking in Inventory Service!');
  }
}

run().catch(console.error);
```

**Setup:**
```bash
cd scripts
npm init -y
npm install axios
node race-condition-test.js
```

---

### Story 8.2 — Structured Logging with MDC (Correlation ID)
**As a developer**, I want structured JSON logs with correlation IDs so I can trace a saga end-to-end.

**Acceptance Criteria:**
- [ ] `logstash-logback-encoder` added to each service's `pom.xml`
- [ ] `logback-spring.xml` configured for JSON output in each service
- [ ] Every `@RabbitListener` sets `MDC.put("orderId", ...)` at the start and `MDC.clear()` at end
- [ ] All log statements use SLF4J (`@Slf4j` Lombok annotation or manual `LoggerFactory`)
- [ ] Running `docker compose logs | grep "ord-abc"` shows the complete saga in sequence

---

### Story 8.3 — Spring Actuator Health Endpoints
**As a developer**, I want each service to expose a detailed health check.

**Acceptance Criteria:**
- [ ] `spring-boot-starter-actuator` in each service's `pom.xml`
- [ ] `management.endpoints.web.exposure.include=health,info` in `application.yml`
- [ ] `GET /actuator/health` returns: `{ "status": "UP", "components": { "db": {...}, "rabbit": {...} } }`
- [ ] Returns `503` if DB or RabbitMQ is down (Spring auto-detects this)
- [ ] Docker Compose `healthcheck` uses this endpoint

**Docker Compose healthcheck example:**
```yaml
order-service:
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
    interval: 10s
    timeout: 5s
    retries: 5
    start_period: 30s
```

---

### Story 8.4 — Database Reset Script
**As a developer**, I want to reset the database between test runs without restarting Docker.

**Acceptance Criteria:**
- [ ] `scripts/reset.js` (Node.js + `pg` library) or `scripts/reset.sql`
- [ ] Resets stock to original seed values for all products
- [ ] Deletes all: `orders`, `order_items`, `payment_attempts`, `reservations`
- [ ] Prints confirmation of rows deleted
- [ ] Safe to run while services are running (uses connection pool)

---

## ✅ Epic 8 Definition of Done

- [ ] `node scripts/race-condition-test.js` prints **PASS** consistently
- [ ] All services emit structured JSON logs with `orderId` field
- [ ] `docker compose logs | grep "<orderId>"` shows full saga trace
- [ ] All services return health status from `/actuator/health`
- [ ] `scripts/reset.js` resets state cleanly for re-testing
