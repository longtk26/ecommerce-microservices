-- V6__add_confirmed_reservation_status.sql
-- Adds CONFIRMED to the stock_reservations status CHECK constraint.
--
-- CONFIRMED = payment succeeded; stock is sold (quantity was already deducted at reservation time).
-- RELEASED  = payment failed; reserved stock was returned to available inventory.
-- RESERVED  = stock is temporarily held, awaiting payment outcome.
-- FAILED    = reservation attempt failed (insufficient stock); nothing was deducted.

ALTER TABLE stock_reservations
    DROP CONSTRAINT IF EXISTS chk_reservation_status;

ALTER TABLE stock_reservations
    ADD CONSTRAINT chk_reservation_status
        CHECK (status IN ('RESERVED', 'CONFIRMED', 'RELEASED', 'FAILED'));
