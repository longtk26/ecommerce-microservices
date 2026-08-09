## Useful command

```bash
# Create a started maven module
mvn archetype:generate \
  -DgroupId=com.ecommerces \
  -DartifactId=shared-events \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false
```


```bash
# Create a spring boot project
curl https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.3.0 \
  -d baseDir=order-service \
  -d groupId=com.ecommerces \
  -d artifactId=order-service \
  -d name=order-service \
  -d packageName=com.ecommerces.order \
  -d dependencies=web,data-jpa,amqp,postgresql,flyway,validation \
  -o order-service.zip

unzip order-service.zip
rm order-service.zip
cd order-service
```