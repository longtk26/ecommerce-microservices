# Epic 2 — Seed Data & Domain Model

## Overview

Before writing any business logic, you need a solid data foundation. This epic defines the **JPA entity models** and **Flyway migration scripts** for all services, then **seeds realistic data** — 2 shops with multiple products and stock levels.

Each microservice owns its own schema. The Inventory Service never queries the Order Service's database. They communicate **only through events**.

By the end of this epic you will have:
- Flyway migration scripts for all 3 databases
- JPA entities for Order, Inventory, and Payment services
- 2 seeded shops with 5+ products each and controlled stock levels

---

## 📐 Domain Model

### Inventory Service (`inventory_db`)

```
shops
──────────────────────────────────────────
id          UUID  PRIMARY KEY
name        VARCHAR(255)
description TEXT
logo_url    VARCHAR(500)
created_at  TIMESTAMP

products
──────────────────────────────────────────
id          UUID  PRIMARY KEY
shop_id     UUID  REFERENCES shops(id)
name        VARCHAR(255)
description TEXT
price       DECIMAL(10,2)
image_url   VARCHAR(500)
created_at  TIMESTAMP

stock
──────────────────────────────────────────
id          UUID  PRIMARY KEY
product_id  UUID  REFERENCES products(id) UNIQUE
quantity    INTEGER  DEFAULT 0
version     INTEGER  DEFAULT 0    ← @Version for JPA Optimistic Locking
updated_at  TIMESTAMP
```

> **Why `version` on stock?** JPA's `@Version` annotation implements Optimistic Locking.  
> When 10 orders read `quantity=3, version=1` simultaneously, only one `UPDATE` will succeed —  
> the others get an `OptimisticLockException` → inventory failed → rollback. This is how you  
> prevent overselling without blocking reads.

---

### Order Service (`orders_db`)

```
orders
──────────────────────────────────────────
id           UUID  PRIMARY KEY
user_id      VARCHAR(255)
shop_id      UUID
status       VARCHAR(20)  CHECK (status IN ('PENDING','COMPLETED','CANCELLED'))
total_amount DECIMAL(10,2)
created_at   TIMESTAMP
updated_at   TIMESTAMP

order_items
──────────────────────────────────────────
id           UUID  PRIMARY KEY
order_id     UUID  REFERENCES orders(id)
product_id   UUID
product_name VARCHAR(255)   ← denormalized snapshot at order time
unit_price   DECIMAL(10,2)  ← snapshot at order time
quantity     INTEGER
```

> **Why denormalize product_name and unit_price?**  
> If the product name or price changes after the order, your order history should show  
> what it was at purchase time. This is a standard e-commerce pattern.

---

### Payment Service (`payments_db`)

```
payment_attempts
──────────────────────────────────────────
id              UUID  PRIMARY KEY
order_id        UUID  UNIQUE        ← one payment per order
amount          DECIMAL(10,2)
status          VARCHAR(10)  CHECK (status IN ('SUCCESS','FAILED'))
failure_reason  VARCHAR(255)
transaction_id  VARCHAR(255)
created_at      TIMESTAMP
```

---

## 🗂️ JPA Entity Examples

### Stock Entity (with @Version)

```java
// inventory-service
@Entity
@Table(name = "stock")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Version                    // ← This is the optimistic lock magic
    @Column(nullable = false)
    private int version;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // getters, setters...
}
```

### Order Entity (with Enum status)

```java
// order-service
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private UUID shopId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

public enum OrderStatus { PENDING, COMPLETED, CANCELLED }
```

---

## 🌱 Seed Data Plan

### Shop 1: "TechNest" (electronics)

| Product | Price | Initial Stock |
|---------|-------|---------------|
| Wireless Earbuds Pro | $49.99 | 50 |
| USB-C Hub 7-in-1 | $29.99 | **3** ← race condition product |
| Mechanical Keyboard | $89.99 | 15 |
| Laptop Stand Aluminum | $39.99 | **0** ← always out of stock |
| Smart LED Desk Lamp | $34.99 | 25 |

### Shop 2: "FreshWear" (apparel)

| Product | Price | Initial Stock |
|---------|-------|---------------|
| Classic White Tee | $19.99 | 100 |
| Slim Fit Chinos | $44.99 | 30 |
| Running Sneakers | $79.99 | 12 |
| Minimalist Watch | $59.99 | 8 |
| Canvas Backpack | $54.99 | 20 |

---

## 📋 Stories

### Story 2.1 — Flyway Migration Scripts
**As a developer**, I want SQL migration files managed by Flyway so the schema is version-controlled and auto-applied on startup.

**Acceptance Criteria:**
- [ ] `services/inventory-service/src/main/resources/db/migration/V1__create_shops.sql`
- [ ] `services/inventory-service/src/main/resources/db/migration/V2__create_products.sql`
- [ ] `services/inventory-service/src/main/resources/db/migration/V3__create_stock.sql`
- [ ] `services/inventory-service/src/main/resources/db/migration/V4__create_reservations.sql`
- [ ] `services/order-service/src/main/resources/db/migration/V1__create_orders.sql`
- [ ] `services/order-service/src/main/resources/db/migration/V2__create_order_items.sql`
- [ ] `services/payment-service/src/main/resources/db/migration/V1__create_payment_attempts.sql`
- [ ] Flyway runs automatically on `spring.flyway.enabled=true` — no manual step needed
- [ ] Migrations are idempotent (`CREATE TABLE IF NOT EXISTS`)

**Flyway naming convention:** `V{version}__{description}.sql` (double underscore)

---

### Story 2.2 — Seed Data via Flyway or ApplicationRunner
**As a developer**, I want realistic seed data inserted on first startup without manual SQL.

**Acceptance Criteria:**
- [ ] Seed data in `V5__seed_data.sql` (Flyway migration) — or alternatively, an `ApplicationRunner` bean in the Inventory Service
- [ ] Inserts both shops, 10 products, and stock records
- [ ] Uses fixed UUIDs so the frontend can reference them reliably across restarts
- [ ] Idempotent: `INSERT ... ON CONFLICT DO NOTHING`
- [ ] Stock for USB-C Hub = **3** (race condition test)
- [ ] Stock for Laptop Stand = **0** (out-of-stock test)

**Recommended approach — Flyway seed migration:**
```sql
-- V5__seed_data.sql (inventory-service)
INSERT INTO shops (id, name, description) VALUES
  ('a1b2c3d4-0001-0001-0001-000000000001', 'TechNest', 'Your destination for tech gadgets'),
  ('a1b2c3d4-0001-0001-0001-000000000002', 'FreshWear', 'Minimalist clothing for modern life')
ON CONFLICT DO NOTHING;

INSERT INTO products (id, shop_id, name, price) VALUES
  ('prod-0001-0001-0001-000000000001', 'a1b2c3d4-0001-0001-0001-000000000001', 'USB-C Hub 7-in-1', 29.99),
  ('prod-0001-0001-0001-000000000002', 'a1b2c3d4-0001-0001-0001-000000000001', 'Laptop Stand Aluminum', 39.99),
  -- ... remaining products
ON CONFLICT DO NOTHING;

INSERT INTO stock (id, product_id, quantity, version) VALUES
  (gen_random_uuid(), 'prod-0001-0001-0001-000000000001', 3,  0),  -- USB-C Hub: 3 (race condition)
  (gen_random_uuid(), 'prod-0001-0001-0001-000000000002', 0,  0),  -- Laptop Stand: 0 (out of stock)
  -- ... remaining stock
ON CONFLICT (product_id) DO NOTHING;
```

---

### Story 2.3 — Inventory Service: Product Listing REST API
**As a buyer (via the frontend)**, I want to fetch all shops and products.

**Acceptance Criteria:**
- [ ] `GET /api/shops` — returns list of all shops
- [ ] `GET /api/shops/{shopId}/products` — returns products with current stock level
- [ ] Response DTOs use records (not entities directly)
- [ ] Products with `stockQuantity = 0` still appear but `inStock = false`
- [ ] Uses Spring Data JPA repositories with a custom JPQL query to join product + stock

**Controller + DTO example:**
```java
@RestController
@RequestMapping("/api/shops")
public class ShopController {

    @GetMapping("/{shopId}/products")
    public List<ProductResponse> getProducts(@PathVariable UUID shopId) {
        return inventoryService.getProductsByShop(shopId);
    }
}

public record ProductResponse(
    UUID id,
    String name,
    String description,
    BigDecimal price,
    String imageUrl,
    int stockQuantity,
    boolean inStock
) {}
```

---

## ✅ Epic 2 Definition of Done

- [ ] All Flyway migrations run on service startup with no errors
- [ ] Service restart re-applies no migrations (Flyway idempotency working)
- [ ] `GET /api/shops` returns both shops
- [ ] `GET /api/shops/{shopId}/products` returns products with stock info
- [ ] USB-C Hub shows `stockQuantity=3`, Laptop Stand shows `stockQuantity=0, inStock=false`
