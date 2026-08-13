package com.enterprise.legacy.api;

import com.enterprise.events.OrderCreatedEvent;
import com.enterprise.legacy.service.LegacyIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for legacy system integration.
 */
@RestController
@RequestMapping("/api/legacy")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Legacy Integration", description = "API for legacy system integration")
public class LegacyIntegrationController {

    private final LegacyIntegrationService legacyIntegrationService;

    /**
     * Process order through legacy systems
     */
    @PostMapping("/process-order")
    @Operation(summary = "Process order through legacy systems",
            description = "Sends order to TIBCO EMS and submits mainframe batch job")
    public ResponseEntity<Map<String, String>> processOrder(@RequestBody OrderCreatedEvent event) {
        log.info("Processing order through legacy | OrderId: {}", event.getOrderId());

        String result = legacyIntegrationService.processOrderThroughLegacy(event);

        return ResponseEntity.ok(Map.of(
                "status", "PROCESSED",
                "legacyReference", result
        ));
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns health status of legacy integration")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "legacy-integration-service",
                "adapters", "TIBCO EMS, Mainframe Batch"
        ));
    }
}
