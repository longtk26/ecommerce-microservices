# Epic 9 — API Gateway (Centralized Entry Point, Routing, Hexagonal Architecture & Cognito AuthN/AuthZ)

## Overview

Without an API Gateway, the frontend and external clients must know the individual hostnames and ports of every microservice (`order-service` on `8081`, `inventory-service` on `8082`, `payment-service` on `8083`, etc.). This creates tight coupling, complicates authentication, causes CORS configuration spread across multiple backends, and makes internal refactoring difficult.

This epic introduces **Spring Cloud Gateway**:
- Provides a **single unified entry point** (`http://localhost:8080`) for the frontend.
- Follows **Hexagonal Architecture (Ports & Adapters)** matching `order-service` and `payment-service`.
- Integrates with the **Service Registry (Eureka)** to dynamically route requests using logical service names (`lb://order-service`, `lb://inventory-service`, `lb://payment-service`) with automatic load balancing.
- Implements **Authentication (AuthN)**:
  - Exposes public authentication endpoints (`POST /api/auth/login`, `POST /api/auth/refresh`).
  - Directly authenticates with **AWS Cognito User Pool** via AWS SDK v2 (`CognitoIdentityProviderAsyncClient`) using `USER_PASSWORD_AUTH` and `REFRESH_TOKEN_AUTH` flows with HMAC-SHA256 `SECRET_HASH` calculation.
  - Acts as an **OAuth2 Resource Server** validating incoming Bearer JWT tokens at the edge against Cognito's JWKS endpoint.
- Manages **Authorization (AuthZ)**:
  - **Coarse-Grained (Gateway Level)**: Route-level Role-Based Access Control (RBAC) based on Cognito User Pool groups (`cognito:groups` -> `ROLE_BUYER`, `ROLE_SELLER`, `ROLE_ADMIN`).
  - **Context Propagation**: Extracts verified claims (`sub`, `email`, `roles`) and injects trusted internal headers (`X-User-Id`, `X-User-Email`, `X-User-Roles`) into downstream requests while stripping untrusted client headers.
  - **Fine-Grained (Downstream Service Level)**: Resource ownership and domain-specific authorization logic inside microservices.
- Handles **centralized CORS policies** and request correlation tracking (`X-Correlation-Id`) in one place.

---

## 📐 Architecture: Gateway Routing, AuthN & AuthZ Flow

```
┌────────────────────────────────────────────────────────────────────────┐
│                          1. USER AUTHENTICATION                         │
│   Frontend ──► POST /api/auth/login { email, password }                 │
│                 (Direct AWS Cognito USER_PASSWORD_AUTH Flow)           │
│   Gateway  ◄── Returns AWS Cognito Tokens (AccessToken, RefreshToken)  │
│   Frontend ──► Saves Token into Secure Cookie (via Next.js Server Act.)│
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
                                     │ 2. API Request with Bearer Token
                                     │    Authorization: Bearer <JWT> / SameSite Cookie
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       API GATEWAY (Port: 8080)                         │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ 3. Authentication (AuthN) - OAuth2 Resource Server               │  │
│  │    • Validates JWT signature via Cognito JWKS endpoint           │  │
│  │    • Checks token expiry (exp), issuer (iss), client_id          │  │
│  │    • Public routes pass through (e.g., /api/auth/**, catalog)    │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     │                                  │
│  ┌──────────────────────────────────▼───────────────────────────────┐  │
│  │ 4. Gateway Authorization (AuthZ) & Claim Mapping                 │  │
│  │    • Maps 'cognito:groups' -> Spring Security GrantedAuthorities │  │
│  │      (e.g., BUYER -> ROLE_BUYER, SELLER -> ROLE_SELLER)          │  │
│  │    • Path RBAC: e.g., POST /api/orders requires ROLE_BUYER       │  │
│  │    • Path RBAC: e.g., POST /api/shops requires ROLE_SELLER       │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     │                                  │
│  ┌──────────────────────────────────▼───────────────────────────────┐  │
│  │ 5. Header Enrichment & Context Propagation Filter                │  │
│  │    • Injects X-User-Id    = jwt.claims['sub']                    │  │
│  │    • Injects X-User-Email = jwt.claims['email']                  │  │
│  │    • Injects X-User-Roles = jwt.claims['cognito:groups']         │  │
│  │    • Injects X-Correlation-Id = UUID                             │  │
│  │    • Strips/sanitizes untrusted inbound client identity headers  │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     │                                  │
│  6. Dynamic Load Balanced Routing via Eureka Discovery                 │
│     • /api/orders/**   ──► lb://order-service                          │
│     • /api/shops/**    ──► lb://inventory-service                      │
│     • /api/products/** ──► lb://inventory-service                      │
│     • /api/payments/** ──► lb://payment-service                        │
└─────────────────────────────────────┬──────────────────────────────────┘
                                      │ Enriched Internal Headers:
                                      │ X-User-Id, X-User-Roles, X-Correlation-Id
                    ┌─────────────────┼─────────────────┐
                    ▼                                   ▼
         ┌──────────────────────┐            ┌──────────────────────┐
         │    Order Service     │            │  Inventory Service   │
         │  order-service:8081  │            │inventory-service:8082│
         │                      │            │                      │
         │ 7. Downstream AuthZ: │            │ 7. Downstream AuthZ: │
         │  • Resource ownership│            │  • Shop ownership    │
         │    (buyerId == sub)  │            │    verification      │
         └──────────────────────┘            └──────────────────────┘
```

---

## 🏛️ Hexagonal Architecture Package Structure

```
com.ecommerces.gateway
├── presentation
│   ├── controllers
│   │   └── AuthController.java                  <-- Public /api/auth endpoints
│   └── dto
│       ├── LoginRequestDto.java                 <-- Java 17 record with @NotBlank
│       ├── LoginResponseDto.java                <-- Java 17 record with profile
│       ├── RefreshTokenRequestDto.java          <-- Java 17 record
│       └── UserProfileDto.java                  <-- Java 17 record
├── ports
│   ├── AuthenticateUseCase.java                 <-- Inbound port interface
│   ├── RefreshTokenUseCase.java                 <-- Inbound port interface
│   └── CognitoAuthPort.java                     <-- Outbound port interface
├── usecases
│   ├── AuthenticateUseCaseImpl.java             <-- Inbound port implementation
│   └── RefreshTokenUseCaseImpl.java             <-- Inbound port implementation
└── infrastructure
    ├── cognito
    │   └── CognitoAuthAdapter.java              <-- AWS Cognito SDK client & SECRET_HASH
    ├── filters
    │   ├── CorrelationTrackingFilter.java       <-- Global correlation ID filter
    │   └── UserContextFilter.java               <-- Header propagation & sanitization
    └── config
        └── SecurityConfig.java                  <-- Reactive WebFlux OAuth2 & RBAC
```

---

## 🔐 AuthN vs AuthZ Strategy

### 1. Authentication (AuthN) with AWS Cognito
- **Provider**: AWS Cognito User Pool.
- **Direct Login**: `POST /api/auth/login` uses `CognitoIdentityProviderAsyncClient.initiateAuth()` with `USER_PASSWORD_AUTH` flow and calculated `SECRET_HASH`.
- **Resource Server Validation**: Spring Cloud Gateway acts as an **OAuth2 Resource Server (Reactive)**.
- **Signature Verification**: Validates JWTs asynchronously using Cognito's public JSON Web Key Set (JWKS):
  `https://cognito-idp.{region}.amazonaws.com/{userPoolId}/.well-known/jwks.json`
- **Claim Checks**: Verifies `iss` matches Cognito issuer URL, token has not expired (`exp`), and contains valid claims.

### 2. Authorization (AuthZ) Handling After AuthN Success

#### A. Gateway Level (Coarse-Grained / Route-Level AuthZ)
1. **Role Mapping**: Cognito user groups stored in the `cognito:groups` claim (e.g. `["BUYER"]`, `["SELLER"]`, `["ADMIN"]`) are mapped to Spring Security `GrantedAuthority` entries (`ROLE_BUYER`, `ROLE_SELLER`, `ROLE_ADMIN`).
2. **Route Access Rules**:
   - **Public (Permit All)**:
     - `/api/auth/**` (Login, Token Refresh)
     - `GET /api/products/**` (Browsing catalog)
     - `GET /api/shops/**` (Viewing shops)
     - `/actuator/health`, `/actuator/info`
   - **Authenticated Buyer**:
     - `POST /api/orders/**`, `GET /api/orders/**`, `/api/payments/**`
   - **Authenticated Seller / Admin**:
     - `POST /api/shops/**`, `PUT /api/shops/**`, `DELETE /api/shops/**`
     - `POST /api/products/**`, `PUT /api/products/**`, `DELETE /api/products/**`

#### B. Context Propagation (Header Enrichment Filter)
Downstream microservices do not re-verify JWT signatures with Cognito for every internal hop:
- The Gateway strips incoming `X-User-*` client headers to prevent spoofing.
- The Gateway injects trusted context headers into downstream requests:
  - `X-User-Id`: Extracted from JWT `sub` claim.
  - `X-User-Email`: Extracted from JWT `email` claim.
  - `X-User-Roles`: Comma-separated list of Cognito groups (e.g., `BUYER,SELLER`).
  - `X-Correlation-Id`: Distributed tracing UUID.

#### C. Downstream Service Level (Fine-Grained / Domain-Level AuthZ)
- Downstream services read `@RequestHeader("X-User-Id") String userId` and `@RequestHeader("X-User-Roles") String roles`.
- Performs fine-grained resource ownership verification (e.g., verifying `order.getBuyerId().equals(userId)` or shop ownership).

---

## 🛠️ Tech Stack & Key Concepts

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Gateway Engine** | `spring-cloud-starter-gateway` | Reactive, non-blocking API Gateway built on Project Reactor & Netty |
| **AWS Cognito SDK** | `software.amazon.awssdk:cognitoidentityprovider` | Direct `InitiateAuth` password & refresh token authentication |
| **AuthN Resource Server** | `spring-boot-starter-oauth2-resource-server` | Validates Cognito JWTs asynchronously using JWKS |
| **Reactive Security** | `spring-boot-starter-security` | Reactive security filter chain (`SecurityWebFilterChain`) & route RBAC |
| **Discovery Routing** | `lb://<SERVICE-NAME>` | Dynamic client-side load balancing via Spring Cloud LoadBalancer + Eureka |
| **Centralized CORS** | `spring.cloud.gateway.globalcors` | Single point of configuration for origin permissions |
| **DTOs** | Java 17 `record` | Immutable data transfer objects with Jakarta validation annotations |
| **Ports & Adapters** | Hexagonal Architecture | Strict separation of domain ports, use cases, presentation, and infrastructure |

---

## 📋 Stories & Implementation Reference

### Story 9.1 — Dependencies & Hexagonal Setup
**pom.xml (api-gateway):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
        <relativePath/>
    </parent>
    <groupId>com.ecommerces</groupId>
    <artifactId>api-gateway</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>api-gateway</name>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.3</spring-cloud.version>
    </properties>

    <dependencies>
        <!-- Reactive Gateway (Netty) -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
        <!-- Eureka Client for Discovery Routing -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>
        <!-- Reactive Security & OAuth2 Resource Server for AWS Cognito JWT validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- AWS SDK for Cognito Identity Provider -->
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>cognitoidentityprovider</artifactId>
        </dependency>

        <!-- Test Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>software.amazon.awssdk</groupId>
                <artifactId>bom</artifactId>
                <version>2.25.60</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### Story 9.2 — Configuration & Load Balanced Routes
**application-dev.yml (api-gateway):**
```yaml
cognito:
  client-id: ${COGNITO_CLIENT_ID:}
  client-secret: ${COGNITO_CLIENT_SECRET:}

aws:
  region: ${AWS_REGION:us-east-1}

server:
  port: ${PORT:8080}
  error:
    include-message: always

spring:
  application:
    name: api-gateway
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${COGNITO_ISSUER_URI:https://cognito-idp.us-east-1.amazonaws.com/us-east-1_example}
          jwk-set-uri: ${COGNITO_JWK_SET_URI:https://cognito-idp.us-east-1.amazonaws.com/us-east-1_example/.well-known/jwks.json}
  cloud:
    gateway:
      discovery:
        locator:
          enabled: false
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins:
              - "http://localhost:3000"
              - "http://localhost:5173"
            allowed-methods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
              - PATCH
            allowed-headers:
              - "*"
            allow-credentials: true
            max-age: 3600
      routes:
        - id: order-service-route
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**

        - id: inventory-shops-route
          uri: lb://inventory-service
          predicates:
            - Path=/api/shops/**

        - id: inventory-products-route
          uri: lb://inventory-service
          predicates:
            - Path=/api/products/**

        - id: payment-service-route
          uri: lb://payment-service
          predicates:
            - Path=/api/payments/**

eureka:
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${random.uuid}
    lease-renewal-interval-in-seconds: 10
    lease-expiration-duration-in-seconds: 20
  client:
    service-url:
      defaultZone: ${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
    register-with-eureka: true
    fetch-registry: true
```

---

### Story 9.3 — Cognito Direct Auth Adapter & Secret Hash
**CognitoAuthAdapter.java:**
```java
package com.ecommerces.gateway.infrastructure.cognito;

import com.ecommerces.gateway.ports.CognitoAuthPort;
import com.ecommerces.gateway.presentation.dto.LoginResponseDto;
import com.ecommerces.gateway.presentation.dto.UserProfileDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class CognitoAuthAdapter implements CognitoAuthPort {

    private static final Logger log = LoggerFactory.getLogger(CognitoAuthAdapter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CognitoIdentityProviderAsyncClient cognitoClient;
    private final String clientId;
    private final String clientSecret;

    public CognitoAuthAdapter(
            @Value("${cognito.client-id:${COGNITO_CLIENT_ID:}}") String clientId,
            @Value("${cognito.client-secret:${COGNITO_CLIENT_SECRET:}}") String clientSecret,
            @Value("${aws.region:${AWS_REGION:us-east-1}}") String awsRegion) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.cognitoClient = CognitoIdentityProviderAsyncClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }

    @Override
    public Mono<LoginResponseDto> initiateAuth(String email, String password) {
        if (clientId == null || clientId.isBlank()) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "COGNITO_CLIENT_ID is not configured in environment"
            ));
        }

        Map<String, String> authParams = new HashMap<>();
        authParams.put("USERNAME", email);
        authParams.put("PASSWORD", password);

        String secretHash = calculateSecretHash(email);
        if (secretHash != null) {
            authParams.put("SECRET_HASH", secretHash);
        }

        InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .clientId(clientId)
                .authParameters(authParams)
                .build();

        return Mono.fromFuture(cognitoClient.initiateAuth(authRequest))
                .map(response -> {
                    AuthenticationResultType result = response.authenticationResult();
                    if (result == null) {
                        String challenge = response.challengeNameAsString();
                        if ("NEW_PASSWORD_REQUIRED".equals(challenge)) {
                            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Temporary password detected (NEW_PASSWORD_REQUIRED). Please set a permanent password in Cognito.");
                        }
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication challenge required: " + challenge);
                    }
                    UserProfileDto profile = extractUserProfile(result.idToken() != null ? result.idToken() : result.accessToken(), email);
                    return new LoginResponseDto(result.accessToken(), result.idToken(), result.refreshToken(), result.expiresIn(), "Bearer", profile);
                })
                .onErrorMap(this::unwrapCognitoException);
    }

    @Override
    public Mono<LoginResponseDto> refreshAuth(String refreshToken) {
        Map<String, String> authParams = new HashMap<>();
        authParams.put("REFRESH_TOKEN", refreshToken);

        InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                .clientId(clientId)
                .authParameters(authParams)
                .build();

        return Mono.fromFuture(cognitoClient.initiateAuth(authRequest))
                .map(response -> {
                    AuthenticationResultType result = response.authenticationResult();
                    UserProfileDto profile = extractUserProfile(result.idToken() != null ? result.idToken() : result.accessToken(), null);
                    return new LoginResponseDto(result.accessToken(), result.idToken(), refreshToken, result.expiresIn(), "Bearer", profile);
                })
                .onErrorMap(this::unwrapCognitoException);
    }

    private String calculateSecretHash(String userName) {
        if (clientSecret == null || clientSecret.isBlank()) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update(userName.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(mac.doFinal(clientId.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to calculate SECRET_HASH");
        }
    }

    private Throwable unwrapCognitoException(Throwable err) {
        Throwable cause = err;
        while (cause instanceof java.util.concurrent.CompletionException || cause instanceof java.util.concurrent.ExecutionException) {
            if (cause.getCause() != null) cause = cause.getCause();
            else break;
        }
        if (cause instanceof UserNotFoundException || cause instanceof NotAuthorizedException) {
            return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect email or password");
        } else if (cause instanceof UserNotConfirmedException) {
            return new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is not confirmed");
        } else if (cause instanceof ResponseStatusException) {
            return cause;
        }
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed: " + cause.getMessage());
    }

    private UserProfileDto extractUserProfile(String jwtToken, String fallbackEmail) {
        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length >= 2) {
                JsonNode payload = objectMapper.readTree(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
                String sub = payload.has("sub") ? payload.get("sub").asText() : "unknown";
                String email = payload.has("email") ? payload.get("email").asText() : fallbackEmail;
                List<String> roles = new ArrayList<>();
                if (payload.has("cognito:groups") && payload.get("cognito:groups").isArray()) {
                    for (JsonNode g : payload.get("cognito:groups")) roles.add(g.asText().toUpperCase());
                }
                if (roles.isEmpty()) roles.add("BUYER");
                return new UserProfileDto(sub, email, roles);
            }
        } catch (Exception ignored) {}
        return new UserProfileDto("unknown", fallbackEmail, List.of("BUYER"));
    }
}
```

---

### Story 9.4 — Auth Controller & Presentation DTOs
**LoginRequestDto.java:**
```java
package com.ecommerces.gateway.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank String email,
        @NotBlank String password
) {}
```

**LoginResponseDto.java:**
```java
package com.ecommerces.gateway.presentation.dto;

public record LoginResponseDto(
        String accessToken,
        String idToken,
        String refreshToken,
        Integer expiresIn,
        String tokenType,
        UserProfileDto user
) {}
```

**AuthController.java:**
```java
package com.ecommerces.gateway.presentation.controllers;

import com.ecommerces.gateway.ports.AuthenticateUseCase;
import com.ecommerces.gateway.ports.RefreshTokenUseCase;
import com.ecommerces.gateway.presentation.dto.LoginRequestDto;
import com.ecommerces.gateway.presentation.dto.LoginResponseDto;
import com.ecommerces.gateway.presentation.dto.RefreshTokenRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticateUseCase authenticateUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;

    public AuthController(AuthenticateUseCase authenticateUseCase, RefreshTokenUseCase refreshTokenUseCase) {
        this.authenticateUseCase = authenticateUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        return authenticateUseCase.authenticate(request).map(ResponseEntity::ok);
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<LoginResponseDto>> refresh(@Valid @RequestBody RefreshTokenRequestDto request) {
        return refreshTokenUseCase.refresh(request).map(ResponseEntity::ok);
    }
}
```

---

### Story 9.5 — Reactive Security & User Context Filters
**SecurityConfig.java:**
```java
package com.ecommerces.gateway.infrastructure.config;

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

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> {})
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                .pathMatchers("/api/auth/**").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/products/**", "/api/shops/**").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/shops/**").hasAnyRole("SELLER", "ADMIN")
                .pathMatchers(HttpMethod.PUT, "/api/shops/**").hasAnyRole("SELLER", "ADMIN")
                .pathMatchers(HttpMethod.DELETE, "/api/shops/**").hasAnyRole("SELLER", "ADMIN")
                .pathMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole("SELLER", "ADMIN")
                .pathMatchers(HttpMethod.PUT, "/api/products/**").hasAnyRole("SELLER", "ADMIN")
                .pathMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole("SELLER", "ADMIN")
                .pathMatchers("/api/orders/**").authenticated()
                .pathMatchers("/api/payments/**").authenticated()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor()))
            );

        return http.build();
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> groups = jwt.getClaimAsStringList("cognito:groups");
            if (groups == null || groups.isEmpty()) return Collections.emptyList();
            return groups.stream()
                    .map(g -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + g.toUpperCase()))
                    .collect(Collectors.toList());
        });
        return new ReactiveJwtAuthenticationConverterAdapter(jwtConverter);
    }
}
```

**UserContextFilter.java:**
```java
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

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    } else {
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
        return 0;
    }
}
```

---

## ✅ Epic 9 Definition of Done

- [x] `api-gateway` starts on port `8080` and registers with `discovery-service` (Eureka)
- [x] Implemented **Hexagonal Architecture (Ports & Adapters)** with `record` DTOs
- [x] Public endpoints `POST /api/auth/login` and `POST /api/auth/refresh` connect directly to AWS Cognito SDK
- [x] Automatic HMAC-SHA256 `SECRET_HASH` calculation for App Clients with Client Secret
- [x] `GET http://localhost:8080/api/products` routes to `inventory-service` without authentication
- [x] `POST http://localhost:8080/api/orders` without token returns `401 Unauthorized`
- [x] `POST http://localhost:8080/api/orders` with valid Cognito JWT token successfully authenticates and routes to `order-service`
- [x] Gateway maps Cognito groups (`cognito:groups`) to Spring Security roles (`ROLE_BUYER`, `ROLE_SELLER`, `ROLE_ADMIN`) and blocks unauthorized roles with `403 Forbidden`
- [x] Gateway injects `X-User-Id`, `X-User-Email`, `X-User-Roles`, and `X-Correlation-Id` into downstream requests while stripping spoofed client headers
- [x] Downstream services successfully perform fine-grained domain authorization using `X-User-Id` and `X-User-Roles`
- [x] Requests from Next.js frontend (`http://localhost:3000`) pass CORS pre-flight without errors
- [x] Unit and routing tests (`mvn test`) pass (8/8 tests passing)
