package com.ecommerces.gateway.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> {}) // Global CORS handled via spring.cloud.gateway.globalcors
            .authorizeExchange(exchanges -> exchanges
                // Public Actuator / Health
                .pathMatchers("/actuator/health", "/actuator/info").permitAll()

                // Public Authentication Endpoints (Login, Refresh)
                .pathMatchers("/api/auth/**").permitAll()

                // Public Read-Only Catalog Endpoints
                .pathMatchers(HttpMethod.GET, "/api/products/**", "/api/shops/**").permitAll()

                // Role-Restricted Admin / Seller Endpoints
                .pathMatchers(HttpMethod.POST, "/api/shops/**").hasAnyRole("SELLER", "ADMIN")
                .pathMatchers(HttpMethod.PUT, "/api/shops/**").hasAnyRole("SELLER", "ADMIN")
                .pathMatchers(HttpMethod.DELETE, "/api/shops/**").hasAnyRole("SELLER", "ADMIN")
                .pathMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole("SELLER", "ADMIN")
                .pathMatchers(HttpMethod.PUT, "/api/products/**").hasAnyRole("SELLER", "ADMIN")
                .pathMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole("SELLER", "ADMIN")

                // Authenticated Operations (Orders, Payments, etc.)
                .pathMatchers("/api/orders/**").authenticated()
                .pathMatchers("/api/payments/**").authenticated()

                // All other API routes require authentication
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor()))
            );

        return http.build();
    }

    /**
     * Extracts Cognito 'cognito:groups' claim and converts each group into a Spring Security 'ROLE_<GROUP>' authority.
     */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> groups = jwt.getClaimAsStringList("cognito:groups");
            if (groups == null || groups.isEmpty()) {
                log.info("[GATEWAY-AUTHZ] 1. SecurityConfig.grantedAuthoritiesExtractor() - No cognito:groups claim found, assigning default authorities");
                return Collections.emptyList();
            }
            List<GrantedAuthority> authorities = groups.stream()
                    .map(group -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + group.toUpperCase()))
                    .collect(Collectors.toList());
            log.info("[GATEWAY-AUTHZ] 1. SecurityConfig.grantedAuthoritiesExtractor() - Extracted groups: {}, mapped to authorities: {}", groups, authorities);
            return authorities;
        });
        return new ReactiveJwtAuthenticationConverterAdapter(jwtConverter);
    }
}
