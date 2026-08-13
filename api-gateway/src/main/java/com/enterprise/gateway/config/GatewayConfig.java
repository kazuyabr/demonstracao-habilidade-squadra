package com.enterprise.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API Gateway routing configuration.
 *
 * Routes all requests to appropriate microservices:
 * - /api/orders/** → Order Service (localhost:8081)
 * - /api/payments/** → Payment Service (localhost:8082)
 * - /api/inventory/** → Inventory Service (localhost:8083)
 * - /api/notifications/** → Notification Service (localhost:8084)
 * - /api/legacy/** → Legacy Integration Service (localhost:8085)
 * - /api/sagas/** → Saga Orchestrator (localhost:8086)
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Order Service
                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8081"))

                // Payment Service
                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8082"))

                // Inventory Service
                .route("inventory-service", r -> r
                        .path("/api/inventory/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8083"))

                // Notification Service
                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8084"))

                // Legacy Integration Service
                .route("legacy-service", r -> r
                        .path("/api/legacy/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8085"))

                // Saga Orchestrator
                .route("saga-orchestrator", r -> r
                        .path("/api/sagas/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8086"))

                .build();
    }
}
