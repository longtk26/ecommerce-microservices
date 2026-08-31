run:
	@echo "Loading .env and starting api-gateway..."
	@export $$(grep -v '^#' .env | xargs) && \
	mvn spring-boot:run -Dspring-boot.run.profiles=dev

test:
	mvn test

build:
	mvn clean package

clean:
	mvn clean

.PHONY: run test build clean
