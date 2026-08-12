package com.earthtrip.identity.application.port.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InvitationUseCase {
    List<InvitationResult> list(UUID tripId, UUID actorUserId);

    InvitationResult get(UUID tripId, UUID invitationId, UUID actorUserId);

    CreatedInvitation create(
            UUID tripId, UUID actorUserId, UUID requestId, String email, String role);

    InvitationResult update(
            UUID tripId,
            UUID invitationId,
            UUID actorUserId,
            String role,
            Instant expiresAt,
            long baseVersion);

    void revoke(UUID tripId, UUID invitationId, UUID actorUserId, long baseVersion);

    CreatedInvitation redeliver(UUID tripId, UUID invitationId, UUID actorUserId, long baseVersion);

    PreviewResult preview(String rawToken);

    void accept(String rawToken, UUID actorUserId);

    void decline(String rawToken);

    record InvitationResult(
            UUID invitationId,
            UUID tripId,
            String email,
            String role,
            String status,
            Instant expiresAt,
            String deliveryStatus,
            Instant lastDeliveredAt,
            Instant createdAt,
            long version) {}

    record CreatedInvitation(InvitationResult invitation, String token, String invitationUrl) {}

    record PreviewResult(
            UUID invitationId,
            UUID tripId,
            String tripTitle,
            LocalDate startDate,
            LocalDate endDate,
            String invitedEmail,
            String role,
            String status,
            Instant expiresAt) {}
}
