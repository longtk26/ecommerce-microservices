-- =========================
-- Create Databases
-- =========================
CREATE DATABASE orders_db;
CREATE DATABASE inventory_db;
CREATE DATABASE payments_db;


-- =========================
-- ORDERS SERVICE
-- =========================
\connect orders_db;

CREATE TABLE orders (
    id              UUID PRIMARY KEY,
    user_id         VARCHAR(100) NOT NULL,
    shop_id         VARCHAR(100) NOT NULL,
    status          VARCHAR(50) NOT NULL, -- CREATED, COMPLETED, CANCELLED
    total_amount    NUMERIC(12,2) NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id          UUID PRIMARY KEY,
    order_id    UUID NOT NULL,
    product_id  VARCHAR(100) NOT NULL,
    quantity    INT NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);


-- =========================
-- INVENTORY SERVICE
-- =========================
\connect inventory_db;

CREATE TABLE inventory (
    product_id  VARCHAR(100) PRIMARY KEY,
    stock       INT NOT NULL,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventory_reservations (
    id          UUID PRIMARY KEY,
    order_id    UUID NOT NULL,
    product_id  VARCHAR(100) NOT NULL,
    quantity    INT NOT NULL,
    status      VARCHAR(50) NOT NULL, -- RESERVED, RELEASED, FAILED
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =========================
-- PAYMENTS SERVICE
-- =========================
\connect payments_db;

CREATE TABLE payments (
    id              UUID PRIMARY KEY,
    order_id        UUID NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    status          VARCHAR(50) NOT NULL, -- PROCESSED, FAILED
    transaction_id  VARCHAR(255),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);