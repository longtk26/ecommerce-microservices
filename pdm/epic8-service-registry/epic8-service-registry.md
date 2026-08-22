# Epic 8 — Service Registry (Service Discovery)

## Overview

In a distributed microservice deployment (Docker Compose, Kubernetes, or multi-host environments), services dynamically spin up with varying container hostnames and ephemeral IP addresses. Hardcoding ports and hostnames (`localhost:8081`, `localhost:8082`) in application configurations breaks scalability, load balancing, and container networking.

This epic introduces **Service Discovery** via **Spring Cloud Netflix Eureka Server**:
- Every backend service (`order-service`, `inventory-service`, `payment-service`, `notification-service`) registers itself on startup with a logical name (`spring.application.name`).
- The registry maintains an active, self-healing registry of healthy instances via periodic heartbeats.
- Other components (such as the API Gateway) query the registry to dynamically resolve destination hosts without hardcoded IPs.

---

## 📐 Architecture: Service Registry & Discovery

```
                         ┌────────────────────────────────────┐
                         │      SERVICE REGISTRY (Eureka)     │
                         │           Port: 8761               │
                         │                                    │
                         │ Registered Services:               │
                         │  • ORDER-SERVICE       (10.0.0.4)  │
                         │  • INVENTORY-SERVICE   (10.0.0.5)  │
                         │  • PAYMENT-SERVICE     (10.0.0.6)  │
                         │  • NOTIFICATION-SERVICE(10.0.0.7)  │
                         └─────────────────▲──────────────────┘
                                           │
                 ┌─────────────────────────┼─────────────────────────┐
                 │ Heartbeat & Register    │ Heartbeat & Register    │
                 │                         │                         │
     ┌───────────┴──────────┐   ┌──────────┴──────────┐   ┌──────────┴──────────┐
     │    Order Service     │   │  Inventory Service  │   │   Payment Service   │
     │  order-service:8081  │   │inventory-service:8082│  │ payment-service:8083│
     └──────────────────────┘   └─────────────────────┘   └─────────────────────┘
```

---

## 🛠️ Tech Stack & Key Concepts

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Registry Server** | `spring-cloud-starter-netflix-eureka-server` | Standalone discovery registry & web dashboard |
| **Registry Client** | `spring-cloud-starter-netflix-eureka-client` | Client library for automatic registration & heartbeat renewal |
| **Spring Cloud Version** | Spring Cloud 2024.x (compatible with Spring Boot 3/4) | Cloud infrastructure BOM |
| **Heartbeat & Eviction** | 30s lease renewal interval (configurable for dev) | Automatically evicts dead instances |

---

## 📋 Stories

### Story 8.1 — Eureka Server Application Setup
**As an infrastructure engineer**, I want a dedicated Spring Boot service running Eureka Server so that microservices can register themselves.

**Acceptance Criteria:**
- [ ] Maven module created at `services/discovery-service/` (or `services/service-registry/`)
- [ ] `pom.xml` includes `spring-cloud-starter-netflix-eureka-server` and `spring-boot-starter-actuator`
- [ ] Main application class annotated with `@EnableEurekaServer`
- [ ] `application.yml` configured on port `8761` with standalone mode (`register-with-eureka: false`, `fetch-registry: false`)
- [ ] Eureka Web Dashboard accessible at `http://localhost:8761`

**pom.xml (discovery-service):**
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
    <artifactId>discovery-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>discovery-service</name>

    <properties>
        <java.version>21</java.version>
        <spring-cloud.version>2023.0.3</spring-cloud.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
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

**DiscoveryServiceApplication.java:**
```java
package com.ecommerces.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}
```

**application.yml (discovery-service):**
```yaml
server:
  port: 8761

spring:
  application:
    name: discovery-service

eureka:
  instance:
    hostname: ${DISCOVERY_HOST:localhost}
  client:
    register-with-eureka: false # Standalone: does not register with itself
    fetch-registry: false       # Standalone: does not pull registry cache
    service-url:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
  server:
    enable-self-preservation: false # For local development: immediately drop offline nodes
    eviction-interval-timer-in-ms: 5000
```

---

### Story 8.2 — Microservice Client Integration
**As a backend developer**, I want backend services (`order-service`, `inventory-service`, `payment-service`, `notification-service`) to register with Eureka automatically on startup.

**Acceptance Criteria:**
- [ ] Add `spring-cloud-starter-netflix-eureka-client` to each service's `pom.xml`
- [ ] Define Spring Cloud dependency management BOM in each service's `pom.xml`
- [ ] Add Eureka configuration block to each service's `application.yml`
- [ ] Configure `prefer-ip-address: true` for container networking compatibility
- [ ] Verify each service registers under its uppercase `spring.application.name` (e.g. `ORDER-SERVICE`)

**Add to each service's pom.xml:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**Configuration block for microservices (application.yml):**
```yaml
spring:
  application:
    name: order-service # (or inventory-service, payment-service, notification-service)

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

### Story 8.3 — Docker Compose Integration & Deployment Verification
**As a DevOps engineer**, I want discovery-service integrated into `docker-compose.yml` with proper service dependencies and startup order.

**Acceptance Criteria:**
- [ ] `discovery-service` defined in `docker-compose.yml` with healthcheck
- [ ] Backend services declare `depends_on.discovery-service.condition: service_healthy` or proper restart policies
- [ ] Environment variable `EUREKA_SERVER_URL=http://discovery-service:8761/eureka/` injected into all backend containers
- [ ] Running `docker compose up` results in all 4 microservices listed as `UP` in `http://localhost:8761`

**docker-compose.yml snippet:**
```yaml
  discovery-service:
    build:
      context: ./services/discovery-service
    ports:
      - "8761:8761"
    environment:
      - DISCOVERY_HOST=discovery-service
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 20s

  order-service:
    # ...
    environment:
      - EUREKA_SERVER_URL=http://discovery-service:8761/eureka/
    depends_on:
      discovery-service:
        condition: service_healthy
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
```

---

## ✅ Epic 8 Definition of Done

- [ ] `discovery-service` runs on port `8761` and exposes the Eureka web UI
- [ ] `ORDER-SERVICE`, `INVENTORY-SERVICE`, `PAYMENT-SERVICE`, and `NOTIFICATION-SERVICE` all appear in the Eureka Dashboard
- [ ] Instance termination triggers registration removal within 20 seconds
- [ ] Service hostnames are discovered dynamically without hardcoded container IP addresses
