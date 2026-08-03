package com.earthtrip.identity.adapter.out.persistence.deletion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_deletion_requests")
class AccountDeletionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "scheduled_deletion_at", nullable = false)
    private Instant scheduledDeletionAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected AccountDeletionJpaEntity() { }

    AccountDeletionJpaEntity(UUID id, String userId, Instant requestedAt, Instant scheduledAt) {
        this.id = id.toString();
        this.userId = userId;
        this.requestedAt = requestedAt;
        this.scheduledDeletionAt = scheduledAt;
    }

    UUID id() { return UUID.fromString(id); }

    Instant requestedAt() { return requestedAt; }

    Instant scheduledDeletionAt() { return scheduledDeletionAt; }

    String status() {
        if (completedAt != null) return "COMPLETED";
        if (cancelledAt != null) return "CANCELLED";
        return "PENDING";
    }

    void cancel(Instant now) {
        if (completedAt == null && cancelledAt == null) {
            cancelledAt = now;
        }
    }
}
