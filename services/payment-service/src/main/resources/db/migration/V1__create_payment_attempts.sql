-- V1__create_payment_attempts.sql
-- Payment attempts — one payment attempt per order

CREATE TABLE IF NOT EXISTS payment_attempts (
    id              UUID PRIMARY KEY,
    order_id        UUID NOT NULL UNIQUE,   -- one payment per order
    amount          DECIMAL(10, 2) NOT NULL,
    status          VARCHAR(10) NOT NULL,
    failure_reason  VARCHAR(255),
    transaction_id  VARCHAR(255),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payment_status CHECK (status IN ('SUCCESS', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_payment_attempts_order_id ON payment_attempts (order_id);
