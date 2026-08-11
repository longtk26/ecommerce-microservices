-- V4__create_reservations.sql
-- Stock reservations track inventory hold during the order saga

CREATE TABLE IF NOT EXISTS stock_reservations (
    id          UUID PRIMARY KEY,
    order_id    UUID NOT NULL,
    product_id  UUID NOT NULL,
    quantity    INTEGER NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reservation_status CHECK (status IN ('RESERVED', 'RELEASED', 'FAILED')),
    CONSTRAINT fk_reservations_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX IF NOT EXISTS idx_reservations_order_id ON stock_reservations (order_id);
