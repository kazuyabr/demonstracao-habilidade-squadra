package com.enterprise.saga.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Beans used by the orchestrator to call the real microservices over HTTP.
 */
@Configuration
public class RestConfig {

    @Bean
    public RestTemplate restTemplate() {
        // HttpComponents supports the PATCH method that the order service uses.
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault());
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(20000);
        return new RestTemplate(factory);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
    }
}
