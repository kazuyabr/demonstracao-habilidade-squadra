# Multi-stage build for Inventory Service
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY inventory-service/pom.xml inventory-service/
RUN mvn dependency:go-offline -pl inventory-service
COPY inventory-service/src inventory-service/src
RUN mvn package -pl inventory-service -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/inventory-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
