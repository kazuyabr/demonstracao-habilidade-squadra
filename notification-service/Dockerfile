# Multi-stage build for Notification Service
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY notification-service/pom.xml notification-service/
RUN mvn dependency:go-offline -pl notification-service
COPY notification-service/src notification-service/src
RUN mvn package -pl notification-service -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/notification-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
