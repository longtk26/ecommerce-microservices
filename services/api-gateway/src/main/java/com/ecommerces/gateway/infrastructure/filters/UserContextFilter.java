package com.ecommerces.gateway.infrastructure.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component
public class UserContextFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(UserContextFilter.class);

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_EMAIL_HEADER = "X-User-Email";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .map(auth -> ((JwtAuthenticationToken) auth).getToken())
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(optionalJwt -> {
                    if (optionalJwt.isPresent()) {
                        Jwt jwt = optionalJwt.get();
                        String userId = jwt.getClaimAsString("sub");
                        String email = jwt.getClaimAsString("email");
                        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
                        String roles = (groups != null && !groups.isEmpty()) ? String.join(",", groups) : "BUYER";

                        ServerHttpRequest mutatedRequest = sanitizeHeaders(exchange.getRequest())
                                .header(USER_ID_HEADER, userId != null ? userId : "")
                                .header(USER_EMAIL_HEADER, email != null ? email : "")
                                .header(USER_ROLES_HEADER, roles)
                                .build();

                        log.info("[GATEWAY-AUTHZ] 2. UserContextFilter.filter() - Authenticated user context attached -> userId: {}, email: {}, roles: {}. Headers ({}, {}, {}) injected for downstream.",
                                userId, email, roles, USER_ID_HEADER, USER_EMAIL_HEADER, USER_ROLES_HEADER);
                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    } else {
                        log.info("[GATEWAY-AUTHZ] UserContextFilter.filter() - Unauthenticated / Public request for path: {}, headers sanitized.", exchange.getRequest().getPath());
                        ServerHttpRequest sanitizedRequest = sanitizeHeaders(exchange.getRequest()).build();
                        return chain.filter(exchange.mutate().request(sanitizedRequest).build());
                    }
                });
    }

    private ServerHttpRequest.Builder sanitizeHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(httpHeaders -> {
                    httpHeaders.remove(USER_ID_HEADER);
                    httpHeaders.remove(USER_EMAIL_HEADER);
                    httpHeaders.remove(USER_ROLES_HEADER);
                });
    }

    @Override
    public int getOrder() {
        return 0; // Executes after CorrelationTrackingFilter
    }
}
