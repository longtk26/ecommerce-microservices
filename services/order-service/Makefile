run:
	@echo "Loading .env and starting app..."
	@export $$(grep -v '^#' .env | xargs) && \
	mvn spring-boot:run -Dspring-boot.run.profiles=dev
test:
	mvn test
build:
	mvn clean package

.PHONY: run development test build