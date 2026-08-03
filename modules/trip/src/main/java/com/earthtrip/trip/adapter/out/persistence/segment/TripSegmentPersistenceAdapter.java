package com.earthtrip.trip.adapter.out.persistence.segment;

import com.earthtrip.trip.application.port.out.TripSegmentStorePort;
import com.earthtrip.trip.domain.TripId;
import com.earthtrip.trip.domain.TripSegment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class TripSegmentPersistenceAdapter implements TripSegmentStorePort {
    private final TripSegmentJpaRepository repository;

    TripSegmentPersistenceAdapter(TripSegmentJpaRepository repository) { this.repository = repository; }

    @Override
    public List<TripSegment> findAll(TripId tripId) {
        return repository.findAllByTripIdOrderBySortOrderAsc(tripId.toString()).stream()
            .map(TripSegmentJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<TripSegment> findById(UUID segmentId) {
        return repository.findById(segmentId.toString()).map(TripSegmentJpaEntity::toDomain);
    }

    @Override
    public TripSegment save(TripSegment segment) {
        TripSegmentJpaEntity entity = repository.findById(segment.id().toString())
            .map(existing -> { existing.apply(segment); return existing; })
            .orElseGet(() -> TripSegmentJpaEntity.from(segment));
        return repository.saveAndFlush(entity).toDomain();
    }

    @Override
    public void delete(UUID segmentId) { repository.deleteById(segmentId.toString()); }
}
