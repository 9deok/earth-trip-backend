package com.earthtrip.identity.adapter.out.persistence.linkedidentity;

import com.earthtrip.identity.application.port.out.AccountIdentityStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "linked_identities")
class LinkedIdentityJpaEntity {
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "provider_email", length = 320)
    private String providerEmail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected LinkedIdentityJpaEntity() {}

    LinkedIdentityJpaEntity(AccountIdentityStorePort.IdentityRecord record) {
        id = record.id().toString();
        userId = record.userId().toString();
        provider = record.provider();
        providerSubject = record.providerSubject();
        createdAt = record.createdAt();
        apply(record);
    }

    void apply(AccountIdentityStorePort.IdentityRecord record) {
        providerEmail = record.providerEmail();
        lastUsedAt = record.lastUsedAt();
    }

    AccountIdentityStorePort.IdentityRecord toRecord() {
        return new AccountIdentityStorePort.IdentityRecord(
                UUID.fromString(id),
                UUID.fromString(userId),
                provider,
                providerSubject,
                providerEmail,
                createdAt,
                lastUsedAt,
                version);
    }
}
