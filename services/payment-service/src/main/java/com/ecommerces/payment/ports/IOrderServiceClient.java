package com.ecommerces.payment.ports;

import com.ecommerces.payment.infrastructure.http.dto.OrderDetailsDto;

/**
 * Port for fetching order data from the order-service via HTTP.
 * Implementations are in the infrastructure layer.
 */
public interface IOrderServiceClient {

    /**
     * Fetches the full order details by {@code orderId}.
     *
     * @param orderId the UUID string of the order
     * @return the order details
     * @throws OrderNotFoundException if the order-service returns 404
     * @throws RuntimeException       if the HTTP call fails for any other reason
     */
    OrderDetailsDto getOrderById(String orderId);

    class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String orderId) {
            super("Order not found: " + orderId);
        }
    }
}
