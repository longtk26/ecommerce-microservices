.PHONY: run build test clean

run:
	export $$(grep -v '^#' .env | xargs) && \
	mvn spring-boot:run

build:
	mvn clean package -DskipTests

test:
	mvn test

clean:
	mvn clean
