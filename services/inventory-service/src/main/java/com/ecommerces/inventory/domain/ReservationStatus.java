package com.ecommerces.inventory.domain;

public enum ReservationStatus {
    /** Stock is held for this order — awaiting payment outcome. */
    RESERVED,
    /** Payment succeeded — stock is committed/sold, quantity already deducted. */
    CONFIRMED,
    /** Payment failed — reservation undone, stock quantity restored. */
    RELEASED,
    /** Reservation never succeeded (insufficient stock). */
    FAILED
}
