-- V2__create_order_items.sql
-- Order items table — denormalized snapshot of product info at order time

CREATE TABLE IF NOT EXISTS order_items (
    id              UUID PRIMARY KEY,
    order_id        UUID NOT NULL,
    product_id      UUID NOT NULL,
    product_name    VARCHAR(255) NOT NULL,
    unit_price      DECIMAL(10, 2) NOT NULL,
    quantity        INTEGER NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);
