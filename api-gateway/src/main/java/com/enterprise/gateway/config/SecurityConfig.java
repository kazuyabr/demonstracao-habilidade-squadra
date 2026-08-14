package com.enterprise.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OAuth2/Keycloak Security Configuration for API Gateway.
 *
 * This configuration:
 * 1. Validates JWT tokens from Keycloak
 * 2. Extracts roles from JWT claims
 * 3. Maps Keycloak roles to Spring Security authorities
 * 4. Protects routes based on roles
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf().disable()
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers("/actuator/info").permitAll()
                .pathMatchers("/actuator/prometheus").permitAll()
                .pathMatchers("/swagger-ui/**").permitAll()
                .pathMatchers("/api-docs/**").permitAll()

                // Order Service - requires CUSTOMER, OPERATOR or ADMIN role
                .pathMatchers("/api/orders/**").hasAnyRole("CUSTOMER", "OPERATOR", "ADMIN")

                // Payment Service - requires OPERATOR or ADMIN role
                .pathMatchers("/api/payments/**").hasAnyRole("OPERATOR", "ADMIN")

                // Inventory Service - requires CUSTOMER, OPERATOR or ADMIN role
                .pathMatchers("/api/inventory/**").hasAnyRole("CUSTOMER", "OPERATOR", "ADMIN")

                // Saga Orchestrator - requires OPERATOR or ADMIN role
                .pathMatchers("/api/sagas/**").hasAnyRole("OPERATOR", "ADMIN")

                // Legacy Integration - requires OPERATOR or ADMIN role
                .pathMatchers("/api/legacy/**").hasAnyRole("OPERATOR", "ADMIN")

                // All other requests require authentication
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    /**
     * Converter to extract roles from Keycloak JWT.
     *
     * Keycloak includes roles in the "realm_access" claim:
     * {
     *   "realm_access": {
     *     "roles": ["ROLE_USER", "ROLE_ADMIN"]
     *   }
     * }
     */
    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }

    /**
     * Converts Keycloak roles to Spring Security GrantedAuthority.
     */
    static class KeycloakRoleConverter implements Converter<Jwt, Flux<GrantedAuthority>> {

        @Override
        public Flux<GrantedAuthority> convert(Jwt jwt) {
            List<GrantedAuthority> authorities = new ArrayList<>();

            // Extract realm roles from Keycloak JWT
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) realmAccess.get("roles");

                authorities.addAll(roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList()));
            }

            // Extract resource roles if needed
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess != null) {
                resourceAccess.forEach((clientId, clientAccess) -> {
                    @SuppressWarnings("unchecked")
                    Map<String, List<String>> clientRoles = (Map<String, List<String>>) clientAccess;
                    if (clientRoles.containsKey("roles")) {
                        authorities.addAll(clientRoles.get("roles").stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .collect(Collectors.toList()));
                    }
                });
            }

            return Flux.fromIterable(authorities);
        }
    }
}
