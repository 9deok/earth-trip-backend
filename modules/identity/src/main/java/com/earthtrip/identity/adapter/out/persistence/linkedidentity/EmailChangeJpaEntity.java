package com.earthtrip.identity.adapter.out.persistence.linkedidentity;

import com.earthtrip.identity.application.port.out.AccountIdentityStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_change_requests")
class EmailChangeJpaEntity {
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "new_email", nullable = false, length = 320)
    private String newEmail;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    protected EmailChangeJpaEntity() {}

    EmailChangeJpaEntity(AccountIdentityStorePort.EmailChangeRecord record) {
        id = record.id().toString();
        userId = record.userId().toString();
        newEmail = record.newEmail();
        tokenHash = record.tokenHash();
        createdAt = record.createdAt();
        apply(record);
    }

    void apply(AccountIdentityStorePort.EmailChangeRecord record) {
        status = record.status();
        expiresAt = record.expiresAt();
        confirmedAt = record.confirmedAt();
    }

    AccountIdentityStorePort.EmailChangeRecord toRecord() {
        return new AccountIdentityStorePort.EmailChangeRecord(
                UUID.fromString(id),
                UUID.fromString(userId),
                newEmail,
                tokenHash,
                status,
                expiresAt,
                createdAt,
                confirmedAt);
    }
}
