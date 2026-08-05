package com.earthtrip.trip.adapter.out.persistence.trip;

import com.earthtrip.trip.application.port.out.LoadTripPort;
import com.earthtrip.trip.application.port.out.SaveTripPort;
import com.earthtrip.trip.domain.Trip;
import com.earthtrip.trip.domain.TripId;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
class TripPersistenceAdapter implements LoadTripPort, SaveTripPort {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    private final TripJpaRepository repository;
    private final TripQuerydslSupport querydslSupport;
    private final ObjectMapper json;

    TripPersistenceAdapter(
        TripJpaRepository repository,
        TripQuerydslSupport querydslSupport,
        ObjectMapper json
    ) {
        this.repository = repository;
        this.querydslSupport = querydslSupport;
        this.json = json;
    }

    @Override
    public Optional<Trip> findById(TripId tripId) {
        return querydslSupport.findById(tripId.toString())
            .map(this::toDomain);
    }

    @Override
    public Trip save(Trip trip) {
        TripJpaEntity entity = repository.findById(trip.id().toString())
            .map(existing -> {
                existing.apply(trip, writeNames(trip.companionNames()));
                return existing;
            })
            .orElseGet(() -> TripJpaEntity.from(trip, writeNames(trip.companionNames())));
        return toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public List<Trip> findAllByOwner(UUID ownerUserId) {
        return repository.findAllByOwnerUserIdOrderByUpdatedAtDesc(ownerUserId.toString()).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<Trip> findAllByIds(List<UUID> tripIds) {
        if (tripIds.isEmpty()) return List.of();
        return repository.findAllById(tripIds.stream().map(UUID::toString).toList()).stream()
            .map(this::toDomain)
            .toList();
    }

    private Trip toDomain(TripJpaEntity entity) {
        try {
            return entity.toDomain(json.readValue(entity.companionNamesJson(), STRING_LIST));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 동행자 이름을 읽을 수 없습니다.", exception);
        }
    }

    private String writeNames(List<String> names) {
        try {
            return json.writeValueAsString(names);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("동행자 이름을 저장할 수 없습니다.", exception);
        }
    }
}
