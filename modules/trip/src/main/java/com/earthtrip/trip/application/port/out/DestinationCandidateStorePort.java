package com.earthtrip.trip.application.port.out;

import com.earthtrip.trip.domain.DestinationCandidate;
import com.earthtrip.trip.domain.TripId;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface DestinationCandidateStorePort {
    List<DestinationCandidate> findAll(TripId tripId);

    Optional<DestinationCandidate> findById(UUID candidateId);

    DestinationCandidate save(DestinationCandidate candidate);

    void delete(UUID candidateId);

    List<PreferenceRecord> preferences(UUID candidateId);

    default Map<UUID, List<PreferenceRecord>> preferences(Collection<UUID> candidateIds) {
        Map<UUID, List<PreferenceRecord>> result = new LinkedHashMap<>();
        for (UUID candidateId : candidateIds) result.put(candidateId, preferences(candidateId));
        return result;
    }

    PreferenceRecord savePreference(UUID candidateId, UUID userId, String preference, Instant now);

    void deletePreference(UUID candidateId, UUID userId);

    record PreferenceRecord(UUID userId, String preference, Instant updatedAt) {}
}
