package com.ecommerces.payment.infrastructure.http;

import com.ecommerces.payment.infrastructure.http.dto.OrderDetailsDto;
import com.ecommerces.payment.ports.IOrderServiceClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * HTTP client that calls the order-service REST API to fetch order details.
 * Propagates the active JWT Bearer token from the Spring Security context.
 */
@Component
public class OrderServiceClient implements IOrderServiceClient {

    private final RestClient restClient;

    public OrderServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.order-service.url:http://order-service}") String orderServiceUrl) {
        this.restClient = restClientBuilder
                .baseUrl(orderServiceUrl)
                .requestInitializer(request -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth instanceof JwtAuthenticationToken jwtAuth) {
                        Jwt token = jwtAuth.getToken();
                        request.getHeaders().setBearerAuth(token.getTokenValue());
                    }
                })
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
