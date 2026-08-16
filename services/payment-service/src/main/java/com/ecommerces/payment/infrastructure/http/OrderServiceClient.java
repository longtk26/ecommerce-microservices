package com.ecommerces.payment.infrastructure.http;

import com.ecommerces.payment.infrastructure.http.dto.OrderDetailsDto;
import com.ecommerces.payment.ports.IOrderServiceClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * HTTP that calls the order-service REST API to fetch order details.
 * Uses Spring 6's {@link RestClient} (blocking, replaces RestTemplate).
 */
@Component
public class OrderServiceClient implements IOrderServiceClient {

    private final RestClient restClient;

    public OrderServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.order-service.url}") String orderServiceUrl) {
        this.restClient = restClientBuilder
                .baseUrl(orderServiceUrl)
                .build();
    }

    @Override
    public OrderDetailsDto getOrderById(String orderId) {
        try {
            return restClient.get()
                    .uri("/api/orders/{orderId}", orderId)
                    .retrieve()
                    .body(OrderDetailsDto.class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new OrderNotFoundException(orderId);
            }
            throw ex;
        }
    }
}
