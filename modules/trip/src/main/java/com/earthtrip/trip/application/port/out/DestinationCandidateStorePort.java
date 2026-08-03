package com.earthtrip.trip.application.port.out;

import com.earthtrip.trip.domain.DestinationCandidate;
import com.earthtrip.trip.domain.TripId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DestinationCandidateStorePort {
    List<DestinationCandidate> findAll(TripId tripId);
    Optional<DestinationCandidate> findById(UUID candidateId);
    DestinationCandidate save(DestinationCandidate candidate);
    void delete(UUID candidateId);
    List<PreferenceRecord> preferences(UUID candidateId);
    PreferenceRecord savePreference(UUID candidateId, UUID userId, String preference, Instant now);
    void deletePreference(UUID candidateId, UUID userId);
    record PreferenceRecord(UUID userId, String preference, Instant updatedAt) { }
}
