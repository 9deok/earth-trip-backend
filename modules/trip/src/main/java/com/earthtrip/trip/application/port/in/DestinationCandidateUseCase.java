package com.earthtrip.trip.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DestinationCandidateUseCase {
    List<CandidateResult> list(UUID tripId, UUID actorUserId);

    CandidateResult create(UUID tripId, UUID actorUserId, CandidateCommand command);

    CandidateResult update(
            UUID tripId, UUID candidateId, UUID actorUserId, CandidateCommand command);

    void delete(UUID tripId, UUID candidateId, UUID actorUserId, long baseVersion);

    PreferenceResult putPreference(
            UUID tripId, UUID candidateId, UUID actorUserId, String preference);

    void deletePreference(UUID tripId, UUID candidateId, UUID actorUserId);

    record CandidateCommand(
            UUID requestId,
            String name,
            String countryCode,
            String placeId,
            BigDecimal latitude,
            BigDecimal longitude,
            String note,
            String status,
            long baseVersion) {}

    record PreferenceResult(UUID userId, String preference, Instant updatedAt) {}

    record CandidateResult(
            UUID candidateId,
            UUID tripId,
            String name,
            String countryCode,
            String placeId,
            BigDecimal latitude,
            BigDecimal longitude,
            String note,
            String status,
            List<PreferenceResult> preferences,
            long version,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt) {}
}
