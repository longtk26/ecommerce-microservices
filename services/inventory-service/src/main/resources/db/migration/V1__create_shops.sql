-- V1__create_shops.sql
-- Shops belong to the inventory service

CREATE TABLE IF NOT EXISTS shops (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    logo_url    VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
