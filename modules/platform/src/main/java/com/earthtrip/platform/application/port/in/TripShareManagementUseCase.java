package com.earthtrip.platform.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TripShareManagementUseCase {

    List<ShareLinkResult> list(UUID tripId, UUID actorUserId);

    ShareLinkResult create(UUID tripId, UUID actorUserId, ShareLinkCommand command);

    ShareLinkResult update(UUID tripId, UUID shareId, UUID actorUserId, ShareLinkCommand command);

    void revoke(UUID tripId, UUID shareId, UUID actorUserId, long baseVersion);

    List<AccessEventResult> accessEvents(UUID tripId, UUID shareId, UUID actorUserId);

    record ShareLinkCommand(
            UUID requestId,
            String name,
            List<String> scopes,
            String password,
            Boolean removePassword,
            Instant expiresAt,
            Boolean removeExpiry,
            long baseVersion) {}

    record ShareLinkResult(
            UUID shareId,
            String name,
            List<String> scopes,
            boolean passwordProtected,
            Instant expiresAt,
            String status,
            String shareToken,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt,
            long version) {}

    record AccessEventResult(UUID eventId, boolean success, String reason, Instant occurredAt) {}
}
