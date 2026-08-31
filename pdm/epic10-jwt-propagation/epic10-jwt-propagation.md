# Epic 10 — End-to-End JWT Propagation & Zero-Trust Microservice Security

## Overview

In **Epic 9**, the API Gateway was established as the centralized edge entry point, authenticating users against AWS Cognito and validating Bearer JWT tokens. However, the internal communication model relied on **Perimeter Security**: the Gateway unpacked the JWT and forwarded unverified plaintext headers (`X-User-Id`, `X-User-Email`, `X-User-Roles`) to downstream services (`order-service`, `inventory-service`, `payment-service`).

While simple, perimeter-only security introduces critical architectural weaknesses:
- **Header Spoofing & Lateral Movement**: If an internal service or container is compromised, forged `X-User-*` headers could be sent directly to microservices, bypassing security controls.
- **Lack of Cryptographic Verification**: Downstream microservices blindly trust incoming plain headers without validating token authenticity, expiration, or signatures.
- **Incompatible with Service Mesh / Multi-Gateway Architectures**: Microservices cannot verify the authenticity of requests originating from asynchronous jobs, event handlers, or secondary gateways.

**Epic 10 transitions the entire system to a Zero-Trust Architecture (Defense in Depth)**:
- The **API Gateway** acts as a **Token Relay**, forwarding the original cryptographically signed `Authorization: Bearer <JWT>` header to all downstream microservices.
- Each downstream microservice (`order-service`, `inventory-service`, `payment-service`) operates as an independent **OAuth2 Resource Server**, validating the JWT signature and claims against the **AWS Cognito JWKS endpoint**.
- Internal authorization, resource ownership (`buyerId == jwt.sub`), and role-based access control (`ROLE_SELLER`, `ROLE_BUYER`, `ROLE_ADMIN`) are enforced natively in each service via Spring Security contexts (`SecurityContextHolder`, `@AuthenticationPrincipal Jwt`).
- Distributed Saga events (RabbitMQ) encapsulate cryptographically grounded user identity in event payloads/headers.

---

## 📐 Architecture: Perimeter Security vs. Zero-Trust JWT Propagation

### 1. Previous Model (Perimeter Security via Headers)
```
[Client] ──(Bearer JWT)──► [API Gateway] ──(Plain X-User-* Headers)──► [Downstream Services]
                                 │
                   (Single Point of Auth Validation)
                   ⚠️ Risk: Services blindly trust headers; no internal token verification.
```

### 2. Epic 10 Zero-Trust Model (End-to-End JWT Propagation)
```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. CLIENT REQUEST                                                      │
│    Frontend ──► POST /api/orders                                       │
│                 Header: "Authorization: Bearer <Cognito JWT>"          │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 2. API GATEWAY (Edge Defense & Routing)                                │
│    • Injects X-Correlation-Id for distributed tracing                  │
│    • Coarse-Grained Edge AuthZ: Validates JWT signature & Route RBAC    │
│    • Token Relay: Preserves & forwards Authorization: Bearer <JWT>     │
│    • Routes dynamically via Eureka (lb://order-service)                │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Forwarded Request with:
                                    │ "Authorization: Bearer <Cognito JWT>"
                                    │ "X-Correlation-Id: <UUID>"
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 3. DOWNSTREAM MICROSERVICE (e.g., Order Service / Inventory Service)   │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ A. Spring Security OAuth2 Resource Server (Zero-Trust)           │  │
│  │    • Validates signature locally against Cognito JWKS URI        │  │
│  │    • Validates token expiration (exp), issuer (iss), client_id   │  │
│  │    • Maps 'cognito:groups' -> Spring Security GrantedAuthorities │  │
│  │      (ROLE_BUYER, ROLE_SELLER, ROLE_ADMIN)                       │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     │ Populates SecurityContextHolder   │
│  ┌──────────────────────────────────▼───────────────────────────────┐  │
│  │ B. Fine-Grained Domain Authorization & Resource Ownership        │  │
│  │    • Extract user ID from @AuthenticationPrincipal Jwt (sub)     │  │
│  │    • Verify resource ownership (e.g. order.buyerId == jwt.sub)   │  │
│  │    • Method Security: @PreAuthorize("hasRole('SELLER')")         │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     │                                  │
│  ┌──────────────────────────────────▼───────────────────────────────┐  │
│  │ C. Saga Event Publishing (RabbitMQ)                              │  │
│  │    • Attaches verified userId & userEmail to OrderCreatedEvent   │  │
│  │    • Propagates Correlation ID & User Context through Saga       │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 🏛️ Microservice Security Architecture

### Role-Based Access Control (RBAC) Matrix

| Microservice | Endpoint | Method | Required Role / Auth | Description |
|---|---|---|---|---|
| **API Gateway** | `/api/auth/**` | POST | `permitAll()` | Public login & refresh endpoints |
| **API Gateway** | `/api/products/**` | GET | `permitAll()` | Public catalog browsing |
| **API Gateway** | `/api/shops/**` | GET | `permitAll()` | Public shop listings |
| **Order Service** | `/api/orders` | POST | `ROLE_BUYER`, `ROLE_ADMIN` | Authenticated checkout |
| **Order Service** | `/api/orders/{orderId}` | GET | `authenticated()` | Buyer gets own order / Admin |
| **Inventory Service**| `/api/shops` | POST, PUT | `ROLE_SELLER`, `ROLE_ADMIN` | Shop management |
| **Inventory Service**| `/api/products` | POST, PUT, DELETE | `ROLE_SELLER`, `ROLE_ADMIN` | Product catalog mutations |
| **Payment Service** | `/api/payments/process` | POST | `authenticated()` | Mock payment processing |

---

## 📦 Technical Design & Implementation Steps

### Step 1: Add Spring Security & OAuth2 Resource Server Dependencies

Add to `order-service/pom.xml`, `inventory-service/pom.xml`, and `payment-service/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

---

### Step 2: Shared / Reusable Security Configuration

Each microservice implements a `SecurityConfig` configured for Spring Security Servlet (MVC) with stateless session management and JWT authentication converter:

```java
package com.ecommerces.order.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/shops/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/shops/**").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers("/api/orders/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> groups = jwt.getClaimAsStringList("cognito:groups");
            if (groups == null || groups.isEmpty()) {
                return Collections.emptyList();
            }
            return groups.stream()
                    .map(group -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + group.toUpperCase()))
                    .collect(Collectors.toList());
        });
        return converter;
    }
}
```

---

### Step 3: Application Configuration across Microservices

Configure Cognito JWKS and Issuer in `application.yml` or `application-dev.yml` for all microservices:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${COGNITO_ISSUER_URI:https://cognito-idp.us-east-1.amazonaws.com/us-east-1_example}
          jwk-set-uri: ${COGNITO_JWK_SET_URI:https://cognito-idp.us-east-1.amazonaws.com/us-east-1_example/.well-known/jwks.json}
```

---

### Step 4: Extracting User Claims in Controllers & Use Cases

In controllers, replace custom header parsing with standard Spring Security annotations:

#### Option A: Using `@AuthenticationPrincipal Jwt`
```java
@PostMapping
public ResponseEntity<CreateOrderResponseDto> createOrder(
        @Valid @RequestBody CreateOrderRequestDto request,
        @AuthenticationPrincipal Jwt jwt) {
    
    String userId = jwt.getClaimAsString("sub");
    String email = jwt.getClaimAsString("email");
    
    CreateOrderCommand command = new CreateOrderCommand(
            userId,
            email,
            request.shopId(),
            request.items()
    );
    
    return ResponseEntity.status(HttpStatus.CREATED).body(createOrderUseCase.execute(command));
}
```

#### Option B: Security Context Helper Component
```java
@Component
public class SecurityUserContext {

    public String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getClaimAsString("sub");
        }
        return "anonymous";
    }

    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getClaimAsString("email");
        }
        return null;
    }

    public List<String> getCurrentUserRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return List.of();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
```

---

### Step 5: Gateway Token Relay Optimization

In `api-gateway`, ensure Spring Cloud Gateway routes automatically relay the incoming `Authorization` header without stripping or tampering with the Bearer token:

```yaml
# services/api-gateway/src/main/resources/application-dev.yml
spring:
  cloud:
    gateway:
      default-filters:
        - TokenRelay
        - name: RequestHeaderModifier
          args:
            addRequestHeader: X-Gateway-Forwarded, true
```

---

## 🧪 Verification Plan & Test Matrix

| # | Test Scenario | Request Details | Expected Result |
|---|---|---|---|
| 1 | **Authenticated Order Placement** | `POST /api/orders` with Valid Buyer JWT | `201 Created`, order created with `buyerId == jwt.sub` |
| 2 | **Seller Product Creation** | `POST /api/products` with Valid Seller JWT (`ROLE_SELLER`) | `201 Created`, product created |
| 3 | **Unauthorized Buyer Action** | `POST /api/products` with Buyer JWT (No `ROLE_SELLER`) | `403 Forbidden` returned directly by `inventory-service` |
| 4 | **Expired / Tampered JWT** | `POST /api/orders` with expired/invalid JWT signature | `401 Unauthorized` returned by Resource Server |
| 5 | **Direct Service Call with JWT** | Direct `POST http://localhost:8081/api/orders` bypassing Gateway | Successfully validates JWT signature via Cognito JWKS |
| 6 | **Direct Service Call with Spoofed Headers** | Direct `POST http://localhost:8081/api/orders` with only `X-User-Id` header (no JWT) | `401 Unauthorized` (Zero-Trust protection prevents spoofing) |

---

## ✅ Definition of Done (DoD)

- [ ] `order-service`, `inventory-service`, and `payment-service` include Spring Security and OAuth2 Resource Server dependencies.
- [ ] Microservices validate JWT tokens locally using Cognito JWKS.
- [ ] Cognito groups (`cognito:groups`) map to `ROLE_<GROUP>` granted authorities in all services.
- [ ] Method security (`@PreAuthorize`) and route matchers enforce RBAC in downstream services.
- [ ] User ID (`sub`) and email are read securely from `JwtAuthenticationToken` / `Jwt` context.
- [ ] Direct unauthenticated or spoofed HTTP requests to downstream services are rejected with `401 Unauthorized`.
- [ ] End-to-end checkout flow succeeds seamlessly via Frontend and API Gateway with valid tokens.
