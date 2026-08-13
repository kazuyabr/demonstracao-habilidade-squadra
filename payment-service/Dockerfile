# Multi-stage build for Payment Service
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY payment-service/pom.xml payment-service/
RUN mvn dependency:go-offline -pl payment-service
COPY payment-service/src payment-service/src
RUN mvn package -pl payment-service -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/payment-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
