package com.earthtrip.trip.adapter.out.persistence.destination;

import com.earthtrip.trip.application.port.out.DestinationCandidateStorePort;
import com.earthtrip.trip.domain.DestinationCandidate;
import com.earthtrip.trip.domain.TripId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class DestinationCandidatePersistenceAdapter implements DestinationCandidateStorePort {
    private final DestinationCandidateJpaRepository candidates;
    private final DestinationPreferenceJpaRepository preferences;
    DestinationCandidatePersistenceAdapter(
        DestinationCandidateJpaRepository candidates, DestinationPreferenceJpaRepository preferences
    ) { this.candidates = candidates; this.preferences = preferences; }
    @Override public List<DestinationCandidate> findAll(TripId tripId) {
        return candidates.findAllByTripIdOrderByCreatedAtAsc(tripId.toString()).stream()
            .map(DestinationCandidateJpaEntity::toDomain).toList();
    }
    @Override public Optional<DestinationCandidate> findById(UUID id) {
        return candidates.findById(id.toString()).map(DestinationCandidateJpaEntity::toDomain);
    }
    @Override public DestinationCandidate save(DestinationCandidate candidate) {
        DestinationCandidateJpaEntity entity = candidates.findById(candidate.id().toString())
            .map(existing -> { existing.apply(candidate); return existing; })
            .orElseGet(() -> new DestinationCandidateJpaEntity(candidate));
        return candidates.saveAndFlush(entity).toDomain();
    }
    @Override public void delete(UUID id) { candidates.deleteById(id.toString()); }
    @Override public List<PreferenceRecord> preferences(UUID id) {
        return preferences.findAllByCandidateId(id.toString()).stream()
            .map(DestinationPreferenceJpaEntity::toRecord).toList();
    }
    @Override public PreferenceRecord savePreference(UUID id, UUID userId, String value, Instant now) {
        DestinationPreferenceId key = new DestinationPreferenceId(id.toString(), userId.toString());
        DestinationPreferenceJpaEntity entity = preferences.findById(key)
            .map(existing -> { existing.apply(value, now); return existing; })
            .orElseGet(() -> new DestinationPreferenceJpaEntity(
                id.toString(), userId.toString(), value, now
            ));
        return preferences.saveAndFlush(entity).toRecord();
    }
    @Override public void deletePreference(UUID id, UUID userId) {
        preferences.deleteById(new DestinationPreferenceId(id.toString(), userId.toString()));
    }
}
