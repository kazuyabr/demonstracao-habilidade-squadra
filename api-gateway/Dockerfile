# Multi-stage build for API Gateway
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
RUN mvn dependency:go-offline -pl api-gateway
COPY api-gateway/src api-gateway/src
RUN mvn package -pl api-gateway -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
