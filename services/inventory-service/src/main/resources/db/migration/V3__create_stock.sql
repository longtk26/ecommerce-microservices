-- V3__create_stock.sql
-- Stock tracks quantity per product, with optimistic locking via 'version'

CREATE TABLE IF NOT EXISTS stock (
    id          UUID PRIMARY KEY,
    product_id  UUID NOT NULL UNIQUE,
    quantity    INTEGER NOT NULL DEFAULT 0,
    version     INTEGER NOT NULL DEFAULT 0,   -- @Version for JPA Optimistic Locking
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_product FOREIGN KEY (product_id) REFERENCES products (id)
);
