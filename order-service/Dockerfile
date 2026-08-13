# Multi-stage build for Order Service
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY order-service/pom.xml order-service/
RUN mvn dependency:go-offline -pl order-service
COPY order-service/src order-service/src
RUN mvn package -pl order-service -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/order-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
