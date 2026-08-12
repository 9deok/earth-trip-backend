package com.earthtrip.trip.application.port.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DateCandidateUseCase {
    List<CandidateResult> list(UUID tripId, UUID actor);

    CandidateResult create(UUID tripId, UUID actor, CandidateCommand command);

    CandidateResult update(UUID tripId, UUID id, UUID actor, CandidateCommand command);

    void delete(UUID tripId, UUID id, UUID actor, long baseVersion);

    AvailabilityResult putAvailability(
            UUID tripId, UUID id, UUID actor, String availability, String note);

    record CandidateCommand(
            UUID requestId,
            LocalDate startDate,
            LocalDate endDate,
            String note,
            String status,
            long baseVersion) {}

    record AvailabilityResult(UUID userId, String availability, String note, Instant updatedAt) {}

    record CandidateResult(
            UUID candidateId,
            UUID tripId,
            LocalDate startDate,
            LocalDate endDate,
            String note,
            String status,
            List<AvailabilityResult> availability,
            long version,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt) {}
}
