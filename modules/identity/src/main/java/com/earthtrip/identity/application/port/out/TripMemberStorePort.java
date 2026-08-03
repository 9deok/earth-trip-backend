package com.earthtrip.identity.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripMemberStorePort {
    List<MemberRecord> findAll(UUID tripId);
    Optional<MemberRecord> findById(UUID memberId);
    Optional<MemberRecord> findByTripAndUser(UUID tripId, UUID userId);
    MemberRecord save(MemberRecord member);
    void delete(UUID memberId);

    record MemberRecord(
        UUID id, UUID tripId, UUID userId, String role, String status,
        Instant joinedAt, Instant updatedAt, long version
    ) { }
}
