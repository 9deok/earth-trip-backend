package com.earthtrip.identity.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationStorePort {
    List<InvitationRecord> findAll(UUID tripId);

    Optional<InvitationRecord> findById(UUID invitationId);

    Optional<InvitationRecord> findByTokenHash(String tokenHash);

    Optional<InvitationRecord> findActive(UUID tripId, String email);

    InvitationRecord save(InvitationRecord record);

    record InvitationRecord(
            UUID id,
            UUID tripId,
            String email,
            String role,
            String tokenHash,
            String status,
            UUID invitedBy,
            UUID invitedUserId,
            Instant expiresAt,
            Instant acceptedAt,
            Instant declinedAt,
            Instant revokedAt,
            Instant lastDeliveredAt,
            String deliveryStatus,
            Instant createdAt,
            Instant updatedAt,
            long version) {}
}
