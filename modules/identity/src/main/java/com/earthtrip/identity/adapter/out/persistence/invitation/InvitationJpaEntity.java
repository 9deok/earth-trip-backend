package com.earthtrip.identity.adapter.out.persistence.invitation;

import com.earthtrip.identity.application.port.out.InvitationStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "trip_invitations")
class InvitationJpaEntity {
    @Id @Column(name = "id", nullable = false, length = 36) private String id;
    @Column(name = "trip_id", nullable = false, length = 36) private String tripId;
    @Column(name = "email", nullable = false, length = 320) private String email;
    @Column(name = "role", nullable = false, length = 20) private String role;
    @Column(name = "token_hash", nullable = false, length = 64) private String tokenHash;
    @Column(name = "status", nullable = false, length = 30) private String status;
    @Column(name = "invited_by", nullable = false, length = 36) private String invitedBy;
    @Column(name = "invited_user_id", length = 36) private String invitedUserId;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "accepted_at") private Instant acceptedAt;
    @Column(name = "declined_at") private Instant declinedAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "last_delivered_at") private Instant lastDeliveredAt;
    @Column(name = "delivery_status", nullable = false, length = 40) private String deliveryStatus;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(name = "version", nullable = false) private long version;
    protected InvitationJpaEntity() { }
    InvitationJpaEntity(InvitationStorePort.InvitationRecord r) { id = r.id().toString(); apply(r); }
    void apply(InvitationStorePort.InvitationRecord r) {
        tripId = r.tripId().toString(); email = r.email(); role = r.role(); tokenHash = r.tokenHash();
        status = r.status(); invitedBy = r.invitedBy().toString();
        invitedUserId = r.invitedUserId() == null ? null : r.invitedUserId().toString();
        expiresAt = r.expiresAt(); acceptedAt = r.acceptedAt(); declinedAt = r.declinedAt();
        revokedAt = r.revokedAt(); lastDeliveredAt = r.lastDeliveredAt();
        deliveryStatus = r.deliveryStatus(); createdAt = r.createdAt(); updatedAt = r.updatedAt();
    }
    InvitationStorePort.InvitationRecord toRecord() {
        return new InvitationStorePort.InvitationRecord(
            UUID.fromString(id), UUID.fromString(tripId), email, role, tokenHash, status,
            UUID.fromString(invitedBy), invitedUserId == null ? null : UUID.fromString(invitedUserId),
            expiresAt, acceptedAt, declinedAt, revokedAt, lastDeliveredAt, deliveryStatus,
            createdAt, updatedAt, version
        );
    }
}
