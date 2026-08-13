package com.enterprise.legacy.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulates Mainframe batch job integration.
 *
 * In a real enterprise environment, mainframes process batch jobs
 * for end-of-day processing, reporting, and bulk operations.
 *
 * This adapter demonstrates:
 * - Batch job submission
 * - Job status checking
 * - Result retrieval
 */
@Component
@Slf4j
public class MainframeBatchAdapter {

    private final Map<String, String> jobStatus = new ConcurrentHashMap<>();

    /**
     * Submit batch job to mainframe
     */
    public String submitBatchJob(String jobType, String payload) {
        String jobId = "MF-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("📤 Submitting mainframe batch job | JobId: {} | Type: {}", jobId, jobType);

        // Simulate job submission
        jobStatus.put(jobId, "SUBMITTED");

        // Simulate processing
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        jobStatus.put(jobId, "PROCESSING");
        log.info("✅ Batch job submitted | JobId: {}", jobId);

        return jobId;
    }

    /**
     * Check job status
     */
    public String getJobStatus(String jobId) {
        log.debug("Checking mainframe job status | JobId: {}", jobId);
        return jobStatus.getOrDefault(jobId, "UNKNOWN");
    }

    /**
     * Get job result
     */
    public String getJobResult(String jobId) {
        log.info("📥 Getting mainframe job result | JobId: {}", jobId);

        // Simulate result generation
        String result = String.format("""
                {
                    "jobId": "%s",
                    "status": "COMPLETED",
                    "result": "SUCCESS",
                    "processedRecords": %d,
                    "timestamp": "%s"
                }""",
                jobId,
                (int) (Math.random() * 1000),
                java.time.LocalDateTime.now()
        );

        log.info("✅ Job result retrieved | JobId: {}", jobId);
        return result;
    }
}
