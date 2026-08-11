-- V5__seed_data.sql
-- Seed data: 2 shops, 10 products, 10 stock records
-- All IDs use UUIDv7 format (time-ordered, version=7, variant=8)
-- Idempotent: INSERT ... ON CONFLICT DO NOTHING

-- =============================================================================
-- SHOPS
-- =============================================================================
INSERT INTO shops (id, name, description, logo_url, created_at) VALUES
    ('01939b4c-0000-7000-8000-000000000001', 'TechNest',  'Your destination for tech gadgets and accessories', NULL, CURRENT_TIMESTAMP),
    ('01939b4c-0000-7000-8000-000000000002', 'FreshWear', 'Minimalist clothing for modern life',                NULL, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- PRODUCTS — TechNest (shop 1)
-- =============================================================================
INSERT INTO products (id, shop_id, name, description, price, image_url, created_at) VALUES
    (
        '01939b4c-0001-7000-8000-000000000001',
        '01939b4c-0000-7000-8000-000000000001',
        'Wireless Earbuds Pro',
        'Premium true-wireless earbuds with active noise cancellation and 30-hour battery life.',
        49.99,
        NULL,
        CURRENT_TIMESTAMP
    ),
    (
        '01939b4c-0001-7000-8000-000000000002',
        '01939b4c-0000-7000-8000-000000000001',
        'USB-C Hub 7-in-1',
        '7-port USB-C hub with HDMI 4K, 3× USB-A, SD card, and 100W PD pass-through.',
        29.99,
        NULL,
        CURRENT_TIMESTAMP
    ),
    (
        '01939b4c-0001-7000-8000-000000000003',
        '01939b4c-0000-7000-8000-000000000001',
        'Mechanical Keyboard',
        'Compact TKL mechanical keyboard with tactile brown switches and RGB backlight.',
        89.99,
        NULL,
        CURRENT_TIMESTAMP
    ),
    (
        '01939b4c-0001-7000-8000-000000000004',
        '01939b4c-0000-7000-8000-000000000001',
        'Laptop Stand Aluminum',
        'Adjustable aluminium laptop stand for ergonomic desk setups, fits 11–17 inch laptops.',
        39.99,
        NULL,
        CURRENT_TIMESTAMP
    ),
    (
        '01939b4c-0001-7000-8000-000000000005',
        '01939b4c-0000-7000-8000-000000000001',
        'Smart LED Desk Lamp',
        'App-controlled LED lamp with adjustable colour temperature and wireless charging base.',
        34.99,
        NULL,
        CURRENT_TIMESTAMP
    )
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- PRODUCTS — FreshWear (shop 2)
-- =============================================================================
INSERT INTO products (id, shop_id, name, description, price, image_url, created_at) VALUES
    (
        '01939b4c-0001-7000-8000-000000000006',
        '01939b4c-0000-7000-8000-000000000002',
        'Classic White Tee',
        '100% organic cotton crew-neck t-shirt, pre-shrunk and available in all sizes.',
        19.99,
        NULL,
        CURRENT_TIMESTAMP
    ),
    (
        '01939b4c-0001-7000-8000-000000000007',
        '01939b4c-0000-7000-8000-000000000002',
        'Slim Fit Chinos',
        'Stretch-cotton slim-fit chino trousers in a versatile khaki tone.',
        44.99,
        NULL,
        CURRENT_TIMESTAMP
    ),
    (
        '01939b4c-0001-7000-8000-000000000008',
        '01939b4c-0000-7000-8000-000000000002',
        'Running Sneakers',
        'Lightweight mesh running shoes with responsive foam sole and breathable upper.',
        79.99,
        NULL,
        CURRENT_TIMESTAMP
    ),
    (
        '01939b4c-0001-7000-8000-000000000009',
        '01939b4c-0000-7000-8000-000000000002',
        'Minimalist Watch',
        'Stainless-steel minimalist watch with sapphire crystal glass and genuine leather strap.',
        59.99,
        NULL,
        CURRENT_TIMESTAMP
    ),
    (
        '01939b4c-0001-7000-8000-00000000000a',
        '01939b4c-0000-7000-8000-000000000002',
        'Canvas Backpack',
        'Waxed canvas backpack with laptop sleeve, water-resistant and built for daily commute.',
        54.99,
        NULL,
        CURRENT_TIMESTAMP
    )
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- STOCK
-- version = 0 (initial, no updates yet)
-- Notable:
--   USB-C Hub 7-in-1  → quantity=3  (race condition test product)
--   Laptop Stand       → quantity=0  (out-of-stock test product)
-- =============================================================================
INSERT INTO stock (id, product_id, quantity, version, updated_at) VALUES
    ('01939b4c-0002-7000-8000-000000000001', '01939b4c-0001-7000-8000-000000000001',  50, 0, CURRENT_TIMESTAMP),  -- Wireless Earbuds Pro
    ('01939b4c-0002-7000-8000-000000000002', '01939b4c-0001-7000-8000-000000000002',   3, 0, CURRENT_TIMESTAMP),  -- USB-C Hub 7-in-1      ← race condition test
    ('01939b4c-0002-7000-8000-000000000003', '01939b4c-0001-7000-8000-000000000003',  15, 0, CURRENT_TIMESTAMP),  -- Mechanical Keyboard
    ('01939b4c-0002-7000-8000-000000000004', '01939b4c-0001-7000-8000-000000000004',   0, 0, CURRENT_TIMESTAMP),  -- Laptop Stand Aluminum  ← always out of stock
    ('01939b4c-0002-7000-8000-000000000005', '01939b4c-0001-7000-8000-000000000005',  25, 0, CURRENT_TIMESTAMP),  -- Smart LED Desk Lamp
    ('01939b4c-0002-7000-8000-000000000006', '01939b4c-0001-7000-8000-000000000006', 100, 0, CURRENT_TIMESTAMP),  -- Classic White Tee
    ('01939b4c-0002-7000-8000-000000000007', '01939b4c-0001-7000-8000-000000000007',  30, 0, CURRENT_TIMESTAMP),  -- Slim Fit Chinos
    ('01939b4c-0002-7000-8000-000000000008', '01939b4c-0001-7000-8000-000000000008',  12, 0, CURRENT_TIMESTAMP),  -- Running Sneakers
    ('01939b4c-0002-7000-8000-000000000009', '01939b4c-0001-7000-8000-000000000009',   8, 0, CURRENT_TIMESTAMP),  -- Minimalist Watch
    ('01939b4c-0002-7000-8000-00000000000a', '01939b4c-0001-7000-8000-00000000000a',  20, 0, CURRENT_TIMESTAMP)   -- Canvas Backpack
ON CONFLICT (product_id) DO NOTHING;
