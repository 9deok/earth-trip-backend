package com.earthtrip.identity.application.service.invitation;

import com.earthtrip.identity.application.port.in.InvitationUseCase;
import com.earthtrip.identity.application.port.out.CredentialPort;
import com.earthtrip.identity.application.port.out.InvitationDeliveryPort;
import com.earthtrip.identity.application.port.out.InvitationStorePort;
import com.earthtrip.identity.application.port.out.TripMemberStorePort;
import com.earthtrip.identity.application.port.out.UserAccountStorePort;
import com.earthtrip.identity.domain.EmailAddress;
import com.earthtrip.identity.domain.UserAccount;
import com.earthtrip.identity.domain.UserId;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.api.TripChangePublisher;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class InvitationService implements InvitationUseCase {
    private static final Duration DEFAULT_TTL = Duration.ofDays(7);
    private final InvitationStorePort invitations;
    private final TripMemberStorePort members;
    private final UserAccountStorePort users;
    private final CredentialPort credentials;
    private final InvitationDeliveryPort delivery;
    private final TripAccess tripAccess;
    private final TripChangePublisher changes;
    private final Clock clock;
    private final String publicBaseUrl;

    InvitationService(
            InvitationStorePort invitations,
            TripMemberStorePort members,
            UserAccountStorePort users,
            CredentialPort credentials,
            InvitationDeliveryPort delivery,
            TripAccess tripAccess,
            Clock clock,
            TripChangePublisher changes,
            @Value("${earthtrip.public-base-url:https://app.earthtrip.local}")
                    String publicBaseUrl) {
        this.invitations = invitations;
        this.members = members;
        this.users = users;
        this.credentials = credentials;
        this.delivery = delivery;
        this.tripAccess = tripAccess;
        this.changes = changes;
        this.clock = clock;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationResult> list(UUID tripId, UUID actorUserId) {
        tripAccess.requireEditor(tripId, actorUserId);
        return invitations.findAll(tripId).stream().map(InvitationService::result).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InvitationResult get(UUID tripId, UUID invitationId, UUID actorUserId) {
        tripAccess.requireEditor(tripId, actorUserId);
        return result(loadInTrip(tripId, invitationId));
    }

    @Override
    public CreatedInvitation create(
            UUID tripId, UUID actorUserId, UUID requestId, String rawEmail, String rawRole) {
        tripAccess.requireOwner(tripId, actorUserId);
        EmailAddress email = new EmailAddress(rawEmail);
        String role = role(rawRole);
        InvitationStorePort.InvitationRecord byId = invitations.findById(requestId).orElse(null);
        if (byId != null) {
            throw EarthTripException.conflict(
                    "IDEMPOTENCY_RESULT_REDACTED", "초대 토큰은 최초 응답에서만 제공됩니다. 새 전송 요청을 사용해 주세요.");
        }
        invitations
                .findActive(tripId, email.value())
                .ifPresent(
                        existing -> {
                            throw EarthTripException.conflict(
                                    "ACTIVE_INVITATION_EXISTS", "이미 진행 중인 초대가 있습니다.");
                        });
        Instant now = clock.instant();
        String rawToken = credentials.newToken();
        InvitationStorePort.InvitationRecord invitation =
                new InvitationStorePort.InvitationRecord(
                        requestId,
                        tripId,
                        email.value(),
                        role,
                        credentials.hashToken(rawToken),
                        "PENDING",
                        actorUserId,
                        null,
                        now.plus(DEFAULT_TTL),
                        null,
                        null,
                        null,
                        null,
                        "PENDING",
                        now,
                        now,
                        0);
        CreatedInvitation created = deliver(invitations.save(invitation), rawToken, now);
        changes.publish(tripId, actorUserId, "CREATED", "INVITATION", requestId);
        return created;
    }

    @Override
    public InvitationResult update(
            UUID tripId,
            UUID invitationId,
            UUID actorUserId,
            String rawRole,
            Instant expiresAt,
            long baseVersion) {
        tripAccess.requireOwner(tripId, actorUserId);
        InvitationStorePort.InvitationRecord current = loadInTrip(tripId, invitationId);
        requirePending(current);
        verifyVersion(current, baseVersion);
        Instant now = clock.instant();
        Instant newExpiry = expiresAt == null ? current.expiresAt() : expiresAt;
        if (!newExpiry.isAfter(now))
            throw EarthTripException.badRequest(
                    "INVALID_INVITATION_EXPIRY", "초대 만료 시각은 현재보다 이후여야 합니다.");
        InvitationResult updated =
                result(
                        invitations.save(
                                copy(
                                        current,
                                        role(rawRole == null ? current.role() : rawRole),
                                        current.tokenHash(),
                                        "PENDING",
                                        current.invitedUserId(),
                                        newExpiry,
                                        current.acceptedAt(),
                                        current.declinedAt(),
                                        current.revokedAt(),
                                        current.lastDeliveredAt(),
                                        current.deliveryStatus(),
                                        now)));
        changes.publish(tripId, actorUserId, "UPDATED", "INVITATION", invitationId);
        return updated;
    }

    @Override
    public void revoke(UUID tripId, UUID invitationId, UUID actorUserId, long baseVersion) {
        tripAccess.requireOwner(tripId, actorUserId);
        InvitationStorePort.InvitationRecord current = loadInTrip(tripId, invitationId);
        requirePending(current);
        verifyVersion(current, baseVersion);
        Instant now = clock.instant();
        invitations.save(
                copy(
                        current,
                        current.role(),
                        current.tokenHash(),
                        "REVOKED",
                        current.invitedUserId(),
                        current.expiresAt(),
                        current.acceptedAt(),
                        current.declinedAt(),
                        now,
                        current.lastDeliveredAt(),
                        current.deliveryStatus(),
                        now));
        changes.publish(tripId, actorUserId, "REVOKED", "INVITATION", invitationId);
    }

    @Override
    public CreatedInvitation redeliver(
            UUID tripId, UUID invitationId, UUID actorUserId, long baseVersion) {
        tripAccess.requireEditor(tripId, actorUserId);
        InvitationStorePort.InvitationRecord current = loadInTrip(tripId, invitationId);
        requirePending(current);
        verifyVersion(current, baseVersion);
        Instant now = clock.instant();
        String token = credentials.newToken();
        InvitationStorePort.InvitationRecord rotated =
                copy(
                        current,
                        current.role(),
                        credentials.hashToken(token),
                        "PENDING",
                        current.invitedUserId(),
                        now.plus(DEFAULT_TTL),
                        current.acceptedAt(),
                        current.declinedAt(),
                        current.revokedAt(),
                        current.lastDeliveredAt(),
                        "PENDING",
                        now);
        CreatedInvitation delivered = deliver(invitations.save(rotated), token, now);
        changes.publish(tripId, actorUserId, "REDELIVERED", "INVITATION", invitationId);
        return delivered;
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewResult preview(String rawToken) {
        InvitationStorePort.InvitationRecord invitation = loadToken(rawToken);
        String status = effectiveStatus(invitation, clock.instant());
        TripAccess.PublicTripResult trip = tripAccess.publicInfo(invitation.tripId());
        return new PreviewResult(
                invitation.id(),
                trip.tripId(),
                trip.title(),
                trip.startDate(),
                trip.endDate(),
                maskEmail(invitation.email()),
                invitation.role(),
                status,
                invitation.expiresAt());
    }

    @Override
    public void accept(String rawToken, UUID actorUserId) {
        InvitationStorePort.InvitationRecord invitation = loadToken(rawToken);
        requireUsable(invitation);
        tripAccess.publicInfo(invitation.tripId());
        UserAccount user =
                users.findById(new UserId(actorUserId))
                        .orElseThrow(
                                () ->
                                        EarthTripException.notFound(
                                                "ACCOUNT_NOT_FOUND", "계정을 찾을 수 없습니다."));
        if (!user.email().value().equals(invitation.email()))
            throw EarthTripException.forbidden(
                    "INVITATION_EMAIL_MISMATCH", "초대를 받은 이메일 계정으로 로그인해 주세요.");
        Instant now = clock.instant();
        members.findByTripAndUser(invitation.tripId(), actorUserId)
                .orElseGet(
                        () ->
                                members.save(
                                        new TripMemberStorePort.MemberRecord(
                                                UUID.randomUUID(),
                                                invitation.tripId(),
                                                actorUserId,
                                                invitation.role(),
                                                "ACTIVE",
                                                now,
                                                now,
                                                0)));
        invitations.save(
                copy(
                        invitation,
                        invitation.role(),
                        invitation.tokenHash(),
                        "ACCEPTED",
                        actorUserId,
                        invitation.expiresAt(),
                        now,
                        invitation.declinedAt(),
                        invitation.revokedAt(),
                        invitation.lastDeliveredAt(),
                        invitation.deliveryStatus(),
                        now));
        changes.publish(
                invitation.tripId(), actorUserId, "ACCEPTED", "INVITATION", invitation.id());
    }

    @Override
    public void decline(String rawToken) {
        InvitationStorePort.InvitationRecord invitation = loadToken(rawToken);
        requireUsable(invitation);
        Instant now = clock.instant();
        invitations.save(
                copy(
                        invitation,
                        invitation.role(),
                        invitation.tokenHash(),
                        "DECLINED",
                        null,
                        invitation.expiresAt(),
                        invitation.acceptedAt(),
                        now,
                        invitation.revokedAt(),
                        invitation.lastDeliveredAt(),
                        invitation.deliveryStatus(),
                        now));
        changes.publish(
                invitation.tripId(),
                invitation.invitedBy(),
                "DECLINED",
                "INVITATION",
                invitation.id(),
                java.util.Map.of("performedByInvitee", true));
    }

    private CreatedInvitation deliver(
            InvitationStorePort.InvitationRecord invitation, String token, Instant now) {
        String url =
                publicBaseUrl
                        + "/invitations?token="
                        + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String status =
                delivery.send(new EmailAddress(invitation.email()), url, invitation.expiresAt())
                        .name();
        InvitationStorePort.InvitationRecord saved =
                invitations.save(
                        copy(
                                invitation,
                                invitation.role(),
                                invitation.tokenHash(),
                                invitation.status(),
                                invitation.invitedUserId(),
                                invitation.expiresAt(),
                                invitation.acceptedAt(),
                                invitation.declinedAt(),
                                invitation.revokedAt(),
                                now,
                                status,
                                now));
        return new CreatedInvitation(result(saved), token, url);
    }

    private InvitationStorePort.InvitationRecord loadToken(String token) {
        if (token == null || token.isBlank()) throw invalidToken();
        return invitations
                .findByTokenHash(credentials.hashToken(token))
                .orElseThrow(InvitationService::invalidToken);
    }

    private InvitationStorePort.InvitationRecord loadInTrip(UUID tripId, UUID id) {
        return invitations
                .findById(id)
                .filter(i -> i.tripId().equals(tripId))
                .orElseThrow(
                        () ->
                                EarthTripException.notFound(
                                        "INVITATION_NOT_FOUND", "초대를 찾을 수 없습니다."));
    }

    private void requireUsable(InvitationStorePort.InvitationRecord i) {
        if (!effectiveStatus(i, clock.instant()).equals("PENDING")) throw invalidToken();
    }

    private static void requirePending(InvitationStorePort.InvitationRecord i) {
        if (!i.status().equals("PENDING"))
            throw EarthTripException.conflict("INVITATION_NOT_PENDING", "대기 중인 초대만 변경할 수 있습니다.");
    }

    private static String effectiveStatus(InvitationStorePort.InvitationRecord i, Instant now) {
        return i.status().equals("PENDING") && !now.isBefore(i.expiresAt())
                ? "EXPIRED"
                : i.status();
    }

    private static String role(String raw) {
        String value = raw == null ? "EDITOR" : raw.strip().toUpperCase(Locale.ROOT);
        if (!value.equals("EDITOR") && !value.equals("VIEWER"))
            throw EarthTripException.badRequest(
                    "INVALID_INVITATION_ROLE", "초대 역할은 EDITOR 또는 VIEWER여야 합니다.");
        return value;
    }

    private static void verifyVersion(InvitationStorePort.InvitationRecord i, long baseVersion) {
        if (i.version() != baseVersion)
            throw new EarthTripException(
                    "VERSION_CONFLICT",
                    409,
                    "다른 초대 변경이 먼저 저장되었습니다.",
                    java.util.Map.of("serverVersion", i.version()));
    }

    private static EarthTripException invalidToken() {
        return EarthTripException.notFound("INVITATION_NOT_FOUND", "초대가 만료되었거나 올바르지 않습니다.");
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***" + email.substring(Math.max(0, at));
        return email.substring(0, 1) + "***" + email.substring(at);
    }

    private static InvitationResult result(InvitationStorePort.InvitationRecord i) {
        return new InvitationResult(
                i.id(),
                i.tripId(),
                i.email(),
                i.role(),
                i.status(),
                i.expiresAt(),
                i.deliveryStatus(),
                i.lastDeliveredAt(),
                i.createdAt(),
                i.version());
    }

    private static InvitationStorePort.InvitationRecord copy(
            InvitationStorePort.InvitationRecord i,
            String role,
            String tokenHash,
            String status,
            UUID invitedUserId,
            Instant expiresAt,
            Instant acceptedAt,
            Instant declinedAt,
            Instant revokedAt,
            Instant lastDeliveredAt,
            String deliveryStatus,
            Instant updatedAt) {
        return new InvitationStorePort.InvitationRecord(
                i.id(),
                i.tripId(),
                i.email(),
                role,
                tokenHash,
                status,
                i.invitedBy(),
                invitedUserId,
                expiresAt,
                acceptedAt,
                declinedAt,
                revokedAt,
                lastDeliveredAt,
                deliveryStatus,
                i.createdAt(),
                updatedAt,
                i.version());
    }
}
