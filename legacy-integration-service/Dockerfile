# Multi-stage build for Legacy Integration Service
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
RUN mvn dependency:go-offline -pl legacy-integration-service -am
COPY events/src events/src
COPY legacy-integration-service/src legacy-integration-service/src
RUN mvn package -pl legacy-integration-service -am -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/legacy-integration-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
