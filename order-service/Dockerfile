# Multi-stage build for Order Service
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY events/pom.xml events/
COPY order-service/pom.xml order-service/
COPY payment-service/pom.xml payment-service/
COPY inventory-service/pom.xml inventory-service/
COPY notification-service/pom.xml notification-service/
COPY legacy-integration-service/pom.xml legacy-integration-service/
COPY saga-orchestrator/pom.xml saga-orchestrator/
COPY api-gateway/pom.xml api-gateway/
RUN mvn dependency:go-offline -pl order-service -am
COPY events/src events/src
COPY order-service/src order-service/src
RUN mvn package -pl order-service -am -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/order-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
