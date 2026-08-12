package com.earthtrip.platform.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface AnalysisJobStorePort {

    Optional<JobRecord> find(UUID jobId);

    JobRecord save(JobRecord job);

    record JobRecord(
            UUID id,
            UUID tripId,
            String targetType,
            UUID targetId,
            Map<String, Object> inputPayload,
            List<Map<String, Object>> suggestions,
            String status,
            UUID confirmationRequestId,
            Map<String, Object> confirmedPayload,
            String failureCode,
            String failureMessage,
            int attemptCount,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt,
            long version) {}
}
