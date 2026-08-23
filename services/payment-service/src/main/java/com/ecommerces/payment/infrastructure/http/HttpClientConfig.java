package com.ecommerces.payment.infrastructure.http;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Provides shared HTTP client infrastructure beans.
 *
 * <p>
 * Spring Boot auto-configures a {@link RestClient.Builder} prototype bean via
 * {@code RestClientAutoConfiguration}, but that autoconfiguration can be absent
 * when only certain starters are on the classpath. Declaring it explicitly here
 * guarantees the bean is always available for injection into
 * {@link OrderServiceClient}.
 */
@Configuration
public class HttpClientConfig {

    /**
     * Exposes a fresh {@link RestClient.Builder} as a Spring-managed bean so that
     * {@link OrderServiceClient} (and any future HTTP adapters) can inject it and
     * apply their own base-URL / interceptor configuration without sharing state.
     */
    @Bean
    @LoadBalanced
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
