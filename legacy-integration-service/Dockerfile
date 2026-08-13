# Multi-stage build for Legacy Integration Service
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY legacy-integration-service/pom.xml legacy-integration-service/
RUN mvn dependency:go-offline -pl legacy-integration-service
COPY legacy-integration-service/src legacy-integration-service/src
RUN mvn package -pl legacy-integration-service -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/legacy-integration-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
