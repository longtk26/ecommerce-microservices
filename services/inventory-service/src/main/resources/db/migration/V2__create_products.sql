-- V2__create_products.sql
-- Products belong to a shop

CREATE TABLE IF NOT EXISTS products (
    id          UUID PRIMARY KEY,
    shop_id     UUID NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    price       DECIMAL(10, 2) NOT NULL,
    image_url   VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_shop FOREIGN KEY (shop_id) REFERENCES shops (id)
);
