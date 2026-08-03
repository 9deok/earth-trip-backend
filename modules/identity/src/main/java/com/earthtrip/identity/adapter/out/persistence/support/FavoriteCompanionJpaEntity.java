package com.earthtrip.identity.adapter.out.persistence.support;

import com.earthtrip.identity.application.port.out.PersonalSupportStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "favorite_companions")
class FavoriteCompanionJpaEntity {

    @Id @Column(name = "id", nullable = false, length = 36)
    private String id;
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    @Column(name = "companion_id", length = 36)
    private String companionId;
    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;
    @Column(name = "email", length = 320)
    private String email;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FavoriteCompanionJpaEntity() { }

    FavoriteCompanionJpaEntity(PersonalSupportStorePort.FavoriteRecord record) {
        id = record.id().toString();
        userId = record.userId().toString();
        companionId = record.companionId() == null ? null : record.companionId().toString();
        displayName = record.displayName();
        email = record.email();
        createdAt = record.createdAt();
    }

    PersonalSupportStorePort.FavoriteRecord toRecord() {
        return new PersonalSupportStorePort.FavoriteRecord(
            UUID.fromString(id), UUID.fromString(userId),
            companionId == null ? null : UUID.fromString(companionId),
            displayName, email, createdAt
        );
    }
}
