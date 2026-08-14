package com.ecommerces.order.infrastructure.http;

import com.ecommerces.order.infrastructure.http.dto.ProductInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * HTTP adapter for the Inventory Service.
 * Used during order creation to fetch the current price and name for each product
 * so they can be snapshotted into the order items.
 */
@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    private final RestClient restClient;

    public InventoryClient(
            @Value("${inventory.service.url:http://localhost:8082}") String baseUrl,
            RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Fetches product information (name + price) from the Inventory Service.
     *
     * @param productId UUID string of the product
     * @return {@link ProductInfo} with name and unit price
     * @throws ResponseStatusException 502 if the Inventory Service is unreachable or returns 4xx/5xx
     */
    public ProductInfo getProduct(String productId) {
        log.debug("Fetching product info for productId={}", productId);
        try {
            return restClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new ResponseStatusException(
                                HttpStatus.valueOf(422),
                                "Product not found in inventory: " + productId);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_GATEWAY,
                                "Inventory Service error for productId: " + productId);
                    })
                    .body(ProductInfo.class);
        } catch (RestClientException ex) {
            log.error("Failed to reach Inventory Service for productId={}: {}", productId, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Inventory Service unreachable. Please try again later.", ex);
        }
    }

    /**
     * Bulk-fetches product information for multiple products in a single HTTP call.
     *
     * <p>Calls {@code GET /api/products?ids=uuid1,uuid2,...} on the Inventory Service,
     * which executes a single IN-clause query instead of one query per product.
     *
     * @param productIds list of product UUID strings
     * @return list of {@link ProductInfo} for the requested products (order not guaranteed)
     * @throws ResponseStatusException 422 if any product ID is invalid, 502 if the service is unreachable
     */
    public List<ProductInfo> getProductsByIds(List<String> productIds) {
        String ids = String.join(",", productIds);
        log.debug("Bulk-fetching product info for {} productIds", productIds.size());
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/products")
                            .queryParam("ids", ids)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new ResponseStatusException(
                                HttpStatus.valueOf(422),
                                "One or more products not found in inventory: " + ids);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_GATEWAY,
                                "Inventory Service error while fetching products: " + ids);
                    })
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException ex) {
            log.error("Failed to reach Inventory Service for productIds={}: {}", ids, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Inventory Service unreachable. Please try again later.", ex);
        }
    }
}
