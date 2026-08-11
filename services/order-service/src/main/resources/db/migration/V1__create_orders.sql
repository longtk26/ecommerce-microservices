-- V1__create_orders.sql
-- Orders table for the Order Service

CREATE TABLE IF NOT EXISTS orders (
    id              UUID PRIMARY KEY,
    user_id         VARCHAR(255) NOT NULL,
    shop_id         UUID NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount    DECIMAL(10, 2) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_order_status CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED'))
);
