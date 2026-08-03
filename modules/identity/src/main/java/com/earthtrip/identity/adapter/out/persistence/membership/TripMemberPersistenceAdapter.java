package com.earthtrip.identity.adapter.out.persistence.membership;

import com.earthtrip.identity.application.port.out.TripMemberStorePort;
import com.earthtrip.trip.spi.TripMembershipLookup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class TripMemberPersistenceAdapter implements TripMemberStorePort, TripMembershipLookup {
    private final TripMemberJpaRepository repository;
    TripMemberPersistenceAdapter(TripMemberJpaRepository repository) { this.repository = repository; }

    @Override public List<MemberRecord> findAll(UUID tripId) {
        return repository.findAllByTripIdOrderByJoinedAtAsc(tripId.toString()).stream()
            .map(TripMemberJpaEntity::toRecord).toList();
    }
    @Override public Optional<MemberRecord> findById(UUID memberId) {
        return repository.findById(memberId.toString()).map(TripMemberJpaEntity::toRecord);
    }
    @Override public Optional<MemberRecord> findByTripAndUser(UUID tripId, UUID userId) {
        return repository.findByTripIdAndUserId(tripId.toString(), userId.toString())
            .map(TripMemberJpaEntity::toRecord);
    }
    @Override public MemberRecord save(MemberRecord member) {
        TripMemberJpaEntity entity = repository.findById(member.id().toString())
            .map(existing -> { existing.apply(member); return existing; })
            .orElseGet(() -> new TripMemberJpaEntity(member));
        return repository.saveAndFlush(entity).toRecord();
    }
    @Override public void delete(UUID memberId) { repository.deleteById(memberId.toString()); }
    @Override public Optional<String> activeRole(UUID tripId, UUID userId) {
        return findByTripAndUser(tripId, userId).filter(m -> m.status().equals("ACTIVE"))
            .map(MemberRecord::role);
    }
    @Override public List<UUID> activeTripIds(UUID userId) {
        return repository.findAllByUserIdAndStatus(userId.toString(), "ACTIVE").stream()
            .map(entity -> entity.toRecord().tripId()).distinct().toList();
    }
}
