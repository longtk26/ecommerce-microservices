# 🛒 E-Commerce Order Fulfillment — Choreography Saga
## Project Documentation Master Guide

> **Goal**: Build a microservice system that demonstrates a **Choreography-based Saga** for distributed transactions — focusing exclusively on the buyer's checkout journey.

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Services** | Java 21 + Spring Boot 3 |
| **Messaging** | RabbitMQ (Spring AMQP) |
| **Database** | PostgreSQL + Spring Data JPA |
| **Build Tool** | Maven |
| **Frontend** | React Router v7 (framework mode) + Vite |
| **Infra** | Docker Compose |

---

## 📐 Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                   FRONTEND (React Router v7)                 │
│              Buyer browses → adds to cart → checks out       │
└──────────────────────────────┬──────────────────────────────┘
                               │ REST API
                               ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         MESSAGE BROKER (RabbitMQ)                    │
│                                                                      │
│  ┌────────────────┐   order.created   ┌───────────────────────┐      │
│  │  Order Service │ ───────────────► │  Inventory Service    │      │
│  │  Spring Boot   │ ◄─────────────── │  Spring Boot          │      │
│  │  POST /orders  │ inventory.failed  │  Reserve / Release    │      │
│  │  GET  /orders  │ inventory.released│  Stock (opt. locking) │      │
│  └────────┬───────┘                  └──────────┬────────────┘      │
│           │ payment.processed                   │ inventory.reserved │
│           │                          ┌──────────▼────────────┐      │
│           └─────────────────────────►│  Payment Service      │      │
│                                      │  Spring Boot (Mock)   │      │
│                                      └──────────┬────────────┘      │
│                              payment.failed /   │                    │
│                              payment.processed  │                    │
│  ┌──────────────────────────────────────────◄───┘                    │
│  │  Notification Service  (Observer — never touched by saga)         │
│  │  Listens: order.* → logs mock email to console                    │
│  └───────────────────────────────────────────────────────────────────┘
└──────────────────────────────────────────────────────────────────────┘
```

---

## 🗺️ Implementation Phases

Work through the epics **in order**. Each phase builds on the last.

| Phase | Epics | What You'll Learn |
|-------|-------|-------------------|
| **Phase 1 — Foundation** | [Epic 1](./epic1-foundation/epic1-foundation.md) | Monorepo, Docker Compose, RabbitMQ, Spring AMQP setup |
| **Phase 2 — Data Layer** | [Epic 2](./epic2-seed-data/epic2-seed-data.md) | JPA entities, Flyway migrations, seeding realistic data |
| **Phase 3 — Core Saga** | [Epic 3](./epic3-order-service/epic3-order-service.md) · [Epic 4](./epic4-inventory-service/epic4-inventory-service.md) · [Epic 5](./epic5-payment-service/epic5-payment-service.md) | Event publishing, consuming, compensation rollbacks |
| **Phase 4 — Observers** | [Epic 6](./epic6-notification-service/epic6-notification-service.md) | Adding features without touching existing code |
| **Phase 5 — Frontend** | [Epic 7](./epic7-frontend/epic7-frontend.md) | React Router v7, buyer UI, real-time order status polling |
| **Phase 6 — Quality** | [Epic 8](./epic8-testing-observability/epic8-testing-observability.md) | Race condition tests, structured logging, health checks |

---

## 📂 Folder Structure (target end state)

```
ecommerces/
├── pdm/                          ← You are here (docs)
├── services/
│   ├── order-service/            ← Spring Boot (Maven)
│   ├── inventory-service/        ← Spring Boot (Maven)
│   ├── payment-service/          ← Spring Boot (Maven)
│   └── notification-service/     ← Spring Boot (Maven, no REST — consumer only)
├── frontend/                     ← React Router v7 (Vite)
├── shared/
│   └── events/                   ← Shared Java library: event routing key constants
├── scripts/
│   └── race-condition-test.js    ← Concurrent order load test (Node.js script)
├── docker-compose.yml
└── .env.example
```

---

## 🧭 Key Concepts You'll Practice

| Concept | Where It Appears |
|---------|-----------------|
| **Choreography Saga** | Epics 3–5: services react to events autonomously |
| **Compensation Transaction** | Epics 4–5: rollback stock / cancel order |
| **Idempotency** | Epics 3–5: handle duplicate events safely |
| **Optimistic Locking** | Epic 4: `@Version` on stock entity |
| **Observer Pattern** | Epic 6: Notification Service |
| **Spring AMQP** | Epic 1: RabbitMQ integration in Spring Boot |
| **Flyway Migrations** | Epic 2: versioned DB schema management |

---

## ✅ Definition of Done (project-level)

- [ ] A buyer can browse products from 2 seeded shops
- [ ] A buyer can place an order through the checkout flow
- [ ] The saga completes successfully (stock reserved → payment → completed)
- [ ] Payment failure triggers stock restoration and order cancellation
- [ ] Out-of-stock triggers immediate order cancellation
- [ ] 10 concurrent orders for 3-stock item correctly fails 7 of them
- [ ] Notification Service logs mock emails for completed/cancelled orders
- [ ] All services start with a single `docker compose up`
