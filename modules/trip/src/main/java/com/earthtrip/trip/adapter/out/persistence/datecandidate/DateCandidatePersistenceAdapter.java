package com.earthtrip.trip.adapter.out.persistence.datecandidate;

import com.earthtrip.trip.application.port.out.DateCandidateStorePort;
import com.earthtrip.trip.domain.DateCandidate;
import com.earthtrip.trip.domain.TripId;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
class DateCandidatePersistenceAdapter implements DateCandidateStorePort {
    private final DateCandidateJpaRepository candidates;
    private final DateAvailabilityJpaRepository availability;

    DateCandidatePersistenceAdapter(DateCandidateJpaRepository c, DateAvailabilityJpaRepository a) {
        candidates = c;
        availability = a;
    }

    @Override
    public List<DateCandidate> findAll(TripId t) {
        return candidates.findAllByTripIdOrderByStartDateAsc(t.toString()).stream()
                .map(DateCandidateJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<DateCandidate> findById(UUID id) {
        return candidates.findById(id.toString()).map(DateCandidateJpaEntity::toDomain);
    }

    @Override
    public DateCandidate save(DateCandidate c) {
        DateCandidateJpaEntity e =
                candidates
                        .findById(c.id().toString())
                        .map(
                                x -> {
                                    x.apply(c);
                                    return x;
                                })
                        .orElseGet(() -> new DateCandidateJpaEntity(c));
        return candidates.saveAndFlush(e).toDomain();
    }

    @Override
    public void delete(UUID id) {
        candidates.deleteById(id.toString());
    }

    @Override
    public List<AvailabilityRecord> availability(UUID id) {
        return availability.findAllByCandidateId(id.toString()).stream()
                .map(DateAvailabilityJpaEntity::toRecord)
                .toList();
    }

    @Override
    public Map<UUID, List<AvailabilityRecord>> availability(Collection<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        Map<UUID, List<AvailabilityRecord>> result = new LinkedHashMap<>();
        for (UUID id : ids) result.put(id, new ArrayList<>());
        for (DateAvailabilityJpaEntity row :
                availability.findAllByCandidateIdIn(ids.stream().map(UUID::toString).toList()))
            result.computeIfAbsent(row.candidateId(), ignored -> new ArrayList<>())
                    .add(row.toRecord());
        return result;
    }

    @Override
    public AvailabilityRecord saveAvailability(
            UUID id, UUID user, String value, String note, Instant now) {
        DateAvailabilityId key = new DateAvailabilityId(id.toString(), user.toString());
        DateAvailabilityJpaEntity e =
                availability
                        .findById(key)
                        .map(
                                x -> {
                                    x.apply(value, note, now);
                                    return x;
                                })
                        .orElseGet(
                                () ->
                                        new DateAvailabilityJpaEntity(
                                                id.toString(), user.toString(), value, note, now));
        return availability.save(e).toRecord();
    }
}
