package com.earthtrip.wallet.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ReservationImportStorePort {

    Optional<JobRecord> findJob(UUID jobId);

    JobRecord saveJob(JobRecord job);

    List<CandidateRecord> findCandidates(UUID jobId);

    Optional<CandidateRecord> findCandidate(UUID candidateId);

    CandidateRecord saveCandidate(CandidateRecord candidate);

    record JobRecord(
        UUID id,
        UUID tripId,
        String sourceType,
        Map<String, Object> sourcePayload,
        String status,
        String failureCode,
        String failureMessage,
        int attemptCount,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        long version
    ) { }

    record CandidateRecord(
        UUID id,
        UUID jobId,
        UUID tripId,
        String title,
        String candidateType,
        Map<String, Object> payload,
        BigDecimal confidence,
        String status,
        UUID reservationId,
        String dismissalReason,
        Instant createdAt,
        Instant updatedAt,
        long version
    ) { }
}
