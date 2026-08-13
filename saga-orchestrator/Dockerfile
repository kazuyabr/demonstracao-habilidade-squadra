# Multi-stage build for Saga Orchestrator
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY saga-orchestrator/pom.xml saga-orchestrator/
RUN mvn dependency:go-offline -pl saga-orchestrator
COPY saga-orchestrator/src saga-orchestrator/src
RUN mvn package -pl saga-orchestrator -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/saga-orchestrator/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
