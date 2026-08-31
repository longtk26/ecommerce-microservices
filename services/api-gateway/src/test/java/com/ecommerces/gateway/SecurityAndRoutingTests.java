package com.ecommerces.gateway;

import com.ecommerces.gateway.infrastructure.filters.CorrelationTrackingFilter;
import com.ecommerces.gateway.infrastructure.filters.UserContextFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class SecurityAndRoutingTests {

    @MockBean
    private ReactiveJwtDecoder jwtDecoder;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CorrelationTrackingFilter correlationTrackingFilter;

    @Autowired
    private UserContextFilter userContextFilter;

    @Test
    void publicHealthEndpoint_PermitsAll() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void protectedOrderEndpoint_WithoutToken_ReturnsUnauthorized() {
        webTestClient.post()
                .uri("/api/orders")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void sellerEndpoint_WithoutToken_ReturnsUnauthorized() {
        webTestClient.post()
                .uri("/api/shops")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void sellerEndpoint_WithBuyerRole_ReturnsForbidden() {
        webTestClient
                .mutateWith(mockJwt().jwt(jwt -> jwt
                        .claim("sub", "buyer-uuid-123")
                        .claim("cognito:groups", List.of("BUYER"))
                ))
                .post()
                .uri("/api/shops")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void correlationTrackingFilter_InjectsCorrelationIdIfMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> capturedCorrelationId = new AtomicReference<>();
        GatewayFilterChain filterChain = ex -> {
            capturedCorrelationId.set(ex.getRequest().getHeaders().getFirst(CorrelationTrackingFilter.CORRELATION_ID_HEADER));
            return Mono.empty();
        };

        correlationTrackingFilter.filter(exchange, filterChain).block();

        assertThat(capturedCorrelationId.get()).isNotNull().isNotBlank();
        assertThat(correlationTrackingFilter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    void userContextFilter_PropagatesClaimsAndSanitizesSpoofedHeaders() {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("sub", "user-uuid-999")
                .claim("email", "buyer@example.com")
                .claim("cognito:groups", List.of("BUYER", "VIP"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Collections.emptyList());

        // Inbound request containing spoofed X-User-Id header from external client
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/orders")
                .header(UserContextFilter.USER_ID_HEADER, "spoofed-user-id")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<HttpHeaders> capturedHeaders = new AtomicReference<>();
        GatewayFilterChain filterChain = ex -> {
            capturedHeaders.set(ex.getRequest().getHeaders());
            return Mono.empty();
        };

        // Execute filter within reactive security context containing the authenticated JWT
        userContextFilter.filter(exchange, filterChain)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(new SecurityContextImpl(auth))))
                .block();

        HttpHeaders headers = capturedHeaders.get();
        assertThat(headers).isNotNull();
        // Injected authenticated sub replaces any spoofed client header
        assertThat(headers.getFirst(UserContextFilter.USER_ID_HEADER)).isEqualTo("user-uuid-999");
        assertThat(headers.getFirst(UserContextFilter.USER_EMAIL_HEADER)).isEqualTo("buyer@example.com");
        assertThat(headers.getFirst(UserContextFilter.USER_ROLES_HEADER)).isEqualTo("BUYER,VIP");
    }

    @Test
    void userContextFilter_UnauthenticatedRequest_SanitizesUserHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products")
                .header(UserContextFilter.USER_ID_HEADER, "malicious-user-id")
                .header(UserContextFilter.USER_EMAIL_HEADER, "fake@email.com")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<HttpHeaders> capturedHeaders = new AtomicReference<>();
        GatewayFilterChain filterChain = ex -> {
            capturedHeaders.set(ex.getRequest().getHeaders());
            return Mono.empty();
        };

        userContextFilter.filter(exchange, filterChain).block();

        HttpHeaders headers = capturedHeaders.get();
        assertThat(headers).isNotNull();
        assertThat(headers.getFirst(UserContextFilter.USER_ID_HEADER)).isNull();
        assertThat(headers.getFirst(UserContextFilter.USER_EMAIL_HEADER)).isNull();
    }
}
