# Epic 9 — API Gateway (Centralized Entry Point & Routing)

## Overview

Without an API Gateway, the frontend and external clients must know the individual hostnames and ports of every microservice (`order-service` on `8081`, `inventory-service` on `8082`, etc.). This creates tight coupling, complicates authentication, causes CORS configuration spread across multiple backends, and makes internal refactoring difficult.

This epic introduces **Spring Cloud Gateway**:
- Provides a **single unified entry point** (`http://localhost:8080`) for the frontend.
- Integrates with the **Service Registry (Eureka)** to dynamically route requests using logical service names (`lb://ORDER-SERVICE`, `lb://INVENTORY-SERVICE`) with automatic load balancing.
- Handles **centralized CORS policies** and request logging in one place.

---

## 📐 Architecture: API Gateway Routing

```
┌─────────────────────────────────────────────────────────────┐
│                    FRONTEND (Buyer UI)                      │
│            Target: http://localhost:8080 (Gateway)          │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP Requests
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 API GATEWAY (Port: 8080)                    │
│                                                             │
│  Route Rules:                                               │
│   • /api/orders/**   ──► lb://order-service:8081            │
│   • /api/shops/**    ──► lb://inventory-service:8082        │
│   • /api/products/** ──► lb://inventory-service:8082        │
│                                                             │
│  Cross-Cutting Features:                                    │
│   • Centralized CORS Management                             │
│   • Request Correlation ID injection (X-Correlation-ID)     │
│   • Dynamic Load Balancing via Eureka Discovery             │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
               ▼                               ▼
    ┌──────────────────────┐        ┌──────────────────────┐
    │    Order Service     │        │  Inventory Service   │
    │  order-service:8081  │        │inventory-service:8082│
    └──────────────────────┘        └──────────────────────┘
```

---

## 🛠️ Tech Stack & Key Concepts

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Gateway Engine** | `spring-cloud-starter-gateway` | Reactive, non-blocking API Gateway built on Project Reactor & Netty |
| **Discovery Routing** | `lb://<SERVICE-NAME>` | Dynamic client-side load balancing via Spring Cloud LoadBalancer + Eureka |
| **Centralized CORS** | `spring.cloud.gateway.globalcors` | Single point of configuration for origin permissions |
| **Correlation Filter** | `GlobalFilter` | Injects `X-Correlation-Id` into every request downstream |

---

## 📋 Stories

### Story 9.1 — Spring Cloud Gateway Setup
**As an infrastructure engineer**, I want a Spring Boot Gateway application that registers as a Eureka client and routes incoming HTTP traffic.

**Acceptance Criteria:**
- [ ] Maven module created at `services/api-gateway/`
- [ ] `pom.xml` includes `spring-cloud-starter-gateway`, `spring-cloud-starter-netflix-eureka-client`, and `spring-boot-starter-actuator`
- [ ] Note: `spring-boot-starter-web` (Tomcat) MUST NOT be included — Spring Cloud Gateway requires the reactive WebFlux/Netty stack
- [ ] Runs on port `8080`

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
        <java.version>21</java.version>
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
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
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

**ApiGatewayApplication.java:**
```java
package com.ecommerces.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
```

---

### Story 9.2 — Route Definitions & Eureka Load Balancing
**As a backend developer**, I want Gateway routing rules configured in `application.yml` targeting registered service IDs via `lb://`.

**Acceptance Criteria:**
- [ ] Route `/api/orders/**` mapped to `lb://order-service`
- [ ] Route `/api/shops/**` and `/api/products/**` mapped to `lb://inventory-service`
- [ ] Health and actuator routes exposed via gateway `/actuator/**`
- [ ] Test with curl: `curl http://localhost:8080/api/shops` routes cleanly to `inventory-service`

**application.yml (api-gateway):**
```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: false # Use explicit routes below for strict API contracts
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

eureka:
  instance:
    prefer-ip-address: true
  client:
    service-url:
      defaultZone: ${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
```

---

### Story 9.3 — Centralized CORS & Logging Global Filter
**As a frontend developer**, I want the Gateway to handle all CORS headers and inject a trace ID so that frontend calls never fail with CORS errors regardless of destination service.

**Acceptance Criteria:**
- [ ] Gateway configures global CORS for frontend origins (`http://localhost:3000`, `http://localhost:5173`)
- [ ] Allowed methods: `GET, POST, PUT, DELETE, OPTIONS, PATCH`
- [ ] GlobalFilter generates and propagates `X-Correlation-Id` header to downstream services
- [ ] Downstream microservices can safely disable redundant per-controller `@CrossOrigin` annotations

**Global CORS Configuration (application.yml):**
```yaml
spring:
  cloud:
    gateway:
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
            allowed-headers:
              - "*"
            allow-credentials: true
            max-age: 3600
```

**Logging & Correlation GlobalFilter (CorrelationTrackingFilter.java):**
```java
package com.ecommerces.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationTrackingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationTrackingFilter.class);
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            request = request.mutate()
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .build();
            log.info("Injected correlation ID: {} for path: {}", correlationId, request.getPath());
        }

        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
```

---

### Story 9.4 — Frontend Client Alignment
**As a frontend developer**, I want the frontend API layer to target only the Gateway URL via environment configuration.

**Acceptance Criteria:**
- [ ] Frontend `.env` or configuration uses `NEXT_PUBLIC_API_URL=http://localhost:8080` (or `VITE_API_URL=http://localhost:8080`)
- [ ] No frontend requests point directly to backend service ports (`8081`, `8082`)
- [ ] Shop fetching, product browsing, and order submission all flow seamlessly through port `8080`

---

## ✅ Epic 9 Definition of Done

- [ ] `api-gateway` starts on port `8080` and registers with `discovery-service`
- [ ] `GET http://localhost:8080/api/shops` successfully routes to `inventory-service`
- [ ] `POST http://localhost:8080/api/orders` successfully routes to `order-service`
- [ ] Requests from frontend (`http://localhost:3000` / `http://localhost:5173`) pass CORS pre-flight without errors
- [ ] `X-Correlation-Id` is appended to all incoming and downstream requests
