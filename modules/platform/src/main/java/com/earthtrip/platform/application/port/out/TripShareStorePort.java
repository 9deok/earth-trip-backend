package com.earthtrip.platform.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripShareStorePort {

    List<ShareRecord> findAll(UUID tripId);

    Optional<ShareRecord> findById(UUID shareId);

    Optional<ShareRecord> findByTokenHash(String tokenHash);

    ShareRecord save(ShareRecord record);

    Optional<PasswordSessionRecord> findPasswordSession(String tokenHash);

    PasswordSessionRecord savePasswordSession(PasswordSessionRecord record);

    AccessRecord appendAccess(AccessRecord record);

    List<AccessRecord> accessEvents(UUID shareId);

    record ShareRecord(
            UUID id,
            UUID tripId,
            String tokenHash,
            String name,
            List<String> scopes,
            String passwordHash,
            UUID projectionUserId,
            Instant expiresAt,
            String status,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt,
            Instant revokedAt,
            long version) {}

    record PasswordSessionRecord(
            String tokenHash, UUID shareId, Instant expiresAt, Instant createdAt) {}

    record AccessRecord(
            UUID eventId, UUID shareId, boolean success, String reason, Instant occurredAt) {}
}
