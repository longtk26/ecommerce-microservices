# =============================================================================
# Root Makefile — ecommerces monorepo
# Delegates to individual service Makefiles or runs cross-cutting commands.
# =============================================================================

# ── Helper: load a service's .env and export vars ────────────────────────────
# Usage: $(call load_env,services/order-service)
define load_env
	$(shell grep -v '^\#' $(1)/.env | xargs)
endef

# ── Infrastructure ────────────────────────────────────────────────────────────

## Start all infrastructure services (PostgreSQL + RabbitMQ) in the background
infra-up:
	@echo "▶ Starting infrastructure (PostgreSQL + RabbitMQ)..."
	docker compose -f docker-compose.dev.yml up -d
	@echo "✔ Infrastructure is up."

## Stop all infrastructure services
infra-down:
	@echo "▶ Stopping infrastructure..."
	docker compose -f docker-compose.dev.yml down
	@echo "✔ Infrastructure is down."

# ── Flyway: migrate (apply all pending migrations) ────────────────────────────

## Apply Flyway migrations for order-service (orders_db)
migrate-order:
	@echo "▶ Running Flyway migrations for order-service..."
	@export $$(grep -v '^\#' services/order-service/.env | xargs) && \
	cd services/order-service && mvn flyway:migrate \
		-Dflyway.url=$${DB_URL} \
		-Dflyway.user=$${DB_USER} \
		-Dflyway.password=$${DB_PASSWORD} \
		-Dflyway.locations=classpath:db/migration \
		-Dspring-boot.run.profiles=dev
	@echo "✔ order-service migrations applied."

## Apply Flyway migrations for inventory-service (inventory_db)
migrate-inventory:
	@echo "▶ Running Flyway migrations for inventory-service..."
	@export $$(grep -v '^\#' services/inventory-service/.env | xargs) && \
	cd services/inventory-service && mvn flyway:migrate \
		-Dflyway.url=$${DB_URL} \
		-Dflyway.user=$${DB_USER} \
		-Dflyway.password=$${DB_PASSWORD} \
		-Dflyway.locations=classpath:db/migration \
		-Dspring-boot.run.profiles=dev
	@echo "✔ inventory-service migrations applied."

## Apply Flyway migrations for payment-service (payments_db)
migrate-payment:
	@echo "▶ Running Flyway migrations for payment-service..."
	@export $$(grep -v '^\#' services/payment-service/.env | xargs) && \
	cd services/payment-service && mvn flyway:migrate \
		-Dflyway.url=$${DB_URL} \
		-Dflyway.user=$${DB_USER} \
		-Dflyway.password=$${DB_PASSWORD} \
		-Dflyway.locations=classpath:db/migration \
		-Dspring-boot.run.profiles=dev
	@echo "✔ payment-service migrations applied."

## Apply Flyway migrations for ALL services
migrate-all: migrate-order migrate-inventory migrate-payment
	@echo "✔ All service migrations applied."

# ── Flyway: seed (V5__seed_data.sql lives in inventory-service only) ──────────

## Seed the inventory database (runs V5__seed_data.sql via Flyway migrate)
## Seed is idempotent — INSERT ... ON CONFLICT DO NOTHING
seed-inventory: migrate-inventory
	@echo "✔ Inventory seed data applied (included in migrate-inventory via V5__seed_data.sql)."

## Run all seeds (currently only inventory has seed data)
seed-all: seed-inventory
	@echo "✔ All seed data applied."

# ── Flyway: info / repair ─────────────────────────────────────────────────────

## Show Flyway migration status for order-service
flyway-info-order:
	@export $$(grep -v '^\#' services/order-service/.env | xargs) && \
	cd services/order-service && mvn flyway:info \
		-Dflyway.url=$${DB_URL} \
		-Dflyway.user=$${DB_USER} \
		-Dflyway.password=$${DB_PASSWORD}

## Show Flyway migration status for inventory-service
flyway-info-inventory:
	@export $$(grep -v '^\#' services/inventory-service/.env | xargs) && \
	cd services/inventory-service && mvn flyway:info \
		-Dflyway.url=$${DB_URL} \
		-Dflyway.user=$${DB_USER} \
		-Dflyway.password=$${DB_PASSWORD}

## Show Flyway migration status for payment-service
flyway-info-payment:
	@export $$(grep -v '^\#' services/payment-service/.env | xargs) && \
	cd services/payment-service && mvn flyway:info \
		-Dflyway.url=$${DB_URL} \
		-Dflyway.user=$${DB_USER} \
		-Dflyway.password=$${DB_PASSWORD}

## Show Flyway migration status for ALL services
flyway-info-all: flyway-info-order flyway-info-inventory flyway-info-payment

# ── Build ─────────────────────────────────────────────────────────────────────

## Compile all services
build-all:
	@echo "▶ Compiling all services..."
	@cd services/discovery-service    && mvn compile -q
	@cd services/order-service        && mvn compile -q
	@cd services/inventory-service    && mvn compile -q
	@cd services/payment-service      && mvn compile -q
	@cd services/notification-service && mvn compile -q
	@echo "✔ All services compiled."

# ── Run (dev) ─────────────────────────────────────────────────────────────────

## Start discovery-service in dev mode
run-discovery:
	@echo "▶ Starting discovery-service..."
	@cd services/discovery-service && \
	mvn spring-boot:run

## Start order-service in dev mode
run-order:
	@echo "▶ Starting order-service..."
	@cd services/order-service && \
	export $$(grep -v '^\#' .env | xargs) && \
	mvn spring-boot:run -Dspring-boot.run.profiles=dev

## Start inventory-service in dev mode
run-inventory:
	@echo "▶ Starting inventory-service..."
	@cd services/inventory-service && \
	export $$(grep -v '^\#' .env | xargs) && \
	mvn spring-boot:run -Dspring-boot.run.profiles=dev

## Start payment-service in dev mode
run-payment:
	@echo "▶ Starting payment-service..."
	@cd services/payment-service && \
	export $$(grep -v '^\#' .env | xargs) && \
	mvn spring-boot:run -Dspring-boot.run.profiles=dev

## Start notification-service in dev mode
run-notification:
	@echo "▶ Starting notification-service..."
	@cd services/notification-service && \
	export $$(grep -v '^\#' .env | xargs) && \
	mvn spring-boot:run -Dspring-boot.run.profiles=dev

## Start frontend Next.js application in dev mode
run-fe:
	@echo "▶ Starting Next.js frontend..."
	@cd fe && pnpm dev

## Build frontend Next.js application
build-fe:
	@echo "▶ Building Next.js frontend..."
	@cd fe && pnpm build

# ── Setup: first-time bootstrap ───────────────────────────────────────────────

## Full first-time setup: start infra → apply all migrations + seed data
setup: infra-up
	@echo "⏳ Waiting for PostgreSQL to be ready..."
	@sleep 5
	@$(MAKE) migrate-all
	@echo ""
	@echo "✅ Setup complete! Databases migrated and seeded."
	@echo "   Run 'make run-discovery', 'make run-order', 'make run-inventory', or 'make run-payment' to start services."

# ── Help ──────────────────────────────────────────────────────────────────────

## Show this help message
help:
	@echo ""
	@echo "ecommerces monorepo — available targets:"
	@echo ""
	@grep -E '^##' Makefile | sed 's/## /  /' | column -t -s ':'
	@echo ""

.PHONY: \
	infra-up infra-down \
	migrate-order migrate-inventory migrate-payment migrate-all \
	seed-inventory seed-all \
	flyway-info-order flyway-info-inventory flyway-info-payment flyway-info-all \
	build-all build-fe \
	run-discovery run-order run-inventory run-payment run-notification run-fe \
	setup help

