package com.enterprise.saga.api;

import com.enterprise.saga.domain.SagaInstance;
import com.enterprise.saga.domain.SagaInstanceRepository;
import com.enterprise.saga.domain.SagaStep;
import com.enterprise.saga.domain.SagaStepRepository;
import com.enterprise.saga.domain.SagaStatus;
import com.enterprise.saga.domain.StepStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for monitoring and managing Saga instances.
 *
 * This API is read-only - sagas are created and managed by events.
 */
@RestController
@RequestMapping("/api/sagas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Saga Orchestrator", description = "API for monitoring Saga transactions")
public class SagaController {

    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;

    /**
     * Get all saga instances
     */
    @GetMapping
    @Operation(summary = "Get all saga instances", description = "Returns a list of all saga instances with their current status")
    public ResponseEntity<List<SagaInstance>> getAllSagas() {
        log.debug("Getting all sagas");
        List<SagaInstance> sagas = sagaInstanceRepository.findAll();
        return ResponseEntity.ok(sagas);
    }

    /**
     * Get saga by ID
     */
    @GetMapping("/{sagaId}")
    @Operation(summary = "Get saga by ID", description = "Returns a saga instance with all its steps")
    public ResponseEntity<SagaInstance> getSagaById(@PathVariable String sagaId) {
        log.debug("Getting saga: {}", sagaId);
        return sagaInstanceRepository.findBySagaId(sagaId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get saga steps
     */
    @GetMapping("/{sagaId}/steps")
    @Operation(summary = "Get saga steps", description = "Returns all steps for a saga instance")
    public ResponseEntity<List<SagaStep>> getSagaSteps(@PathVariable String sagaId) {
        log.debug("Getting steps for saga: {}", sagaId);
        List<SagaStep> steps = sagaStepRepository.findBySagaIdOrdered(sagaId);
        return ResponseEntity.ok(steps);
    }

    /**
     * Get saga statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get saga statistics", description = "Returns statistics about saga instances")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.debug("Getting saga stats");

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", sagaInstanceRepository.count());
        stats.put("created", sagaInstanceRepository.countByStatus(SagaStatus.CREATED));
        stats.put("inProgress", sagaInstanceRepository.countByStatus(SagaStatus.IN_PROGRESS));
        stats.put("completed", sagaInstanceRepository.countByStatus(SagaStatus.COMPLETED));
        stats.put("compensating", sagaInstanceRepository.countByStatus(SagaStatus.COMPENSATING));
        stats.put("compensated", sagaInstanceRepository.countByStatus(SagaStatus.COMPENSATED));
        stats.put("failed", sagaInstanceRepository.countByStatus(SagaStatus.FAILED));

        return ResponseEntity.ok(stats);
    }

    /**
     * Get sagas by status
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get sagas by status", description = "Returns all sagas with a specific status")
    public ResponseEntity<List<SagaInstance>> getSagasByStatus(@PathVariable SagaStatus status) {
        log.debug("Getting sagas with status: {}", status);
        List<SagaInstance> sagas = sagaInstanceRepository.findByStatus(status);
        return ResponseEntity.ok(sagas);
    }

    /**
     * Get saga by order ID
     */
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get saga by order ID", description = "Returns the saga instance for a specific order")
    public ResponseEntity<SagaInstance> getSagaByOrderId(@PathVariable String orderId) {
        log.debug("Getting saga for order: {}", orderId);
        return sagaInstanceRepository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
