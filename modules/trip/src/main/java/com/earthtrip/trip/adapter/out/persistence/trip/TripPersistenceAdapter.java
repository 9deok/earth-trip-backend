package com.earthtrip.trip.adapter.out.persistence.trip;

import com.earthtrip.trip.application.port.out.LoadTripPort;
import com.earthtrip.trip.application.port.out.SaveTripPort;
import com.earthtrip.trip.domain.Trip;
import com.earthtrip.trip.domain.TripId;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class TripPersistenceAdapter implements LoadTripPort, SaveTripPort {

    private final TripJpaRepository repository;
    private final TripQuerydslSupport querydslSupport;

    TripPersistenceAdapter(
        TripJpaRepository repository,
        TripQuerydslSupport querydslSupport
    ) {
        this.repository = repository;
        this.querydslSupport = querydslSupport;
    }

    @Override
    public Optional<Trip> findById(TripId tripId) {
        return querydslSupport.findById(tripId.toString())
            .map(TripJpaEntity::toDomain);
    }

    @Override
    public Trip save(Trip trip) {
        TripJpaEntity entity = repository.findById(trip.id().toString())
            .map(existing -> {
                existing.apply(trip);
                return existing;
            })
            .orElseGet(() -> TripJpaEntity.from(trip));
        return repository.saveAndFlush(entity).toDomain();
    }

    @Override
    public List<Trip> findAllByOwner(UUID ownerUserId) {
        return repository.findAllByOwnerUserIdOrderByUpdatedAtDesc(ownerUserId.toString()).stream()
            .map(TripJpaEntity::toDomain)
            .toList();
    }

    @Override
    public List<Trip> findAllByIds(List<UUID> tripIds) {
        if (tripIds.isEmpty()) return List.of();
        return repository.findAllById(tripIds.stream().map(UUID::toString).toList()).stream()
            .map(TripJpaEntity::toDomain)
            .toList();
    }
}
