package com.earthtrip.wallet.adapter.out.persistence.template;

import com.earthtrip.wallet.application.port.out.PackingTemplateStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "packing_templates")
class PackingTemplateJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "visibility", nullable = false, length = 20)
    private String visibility;

    @Column(name = "items", nullable = false, columnDefinition = "JSON")
    private String items;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PackingTemplateJpaEntity() {}

    PackingTemplateJpaEntity(PackingTemplateStorePort.TemplateRecord record, String items) {
        id = record.id().toString();
        userId = record.userId().toString();
        createdAt = record.createdAt();
        apply(record, items);
    }

    void apply(PackingTemplateStorePort.TemplateRecord record, String items) {
        name = record.name();
        visibility = record.visibility();
        this.items = items;
        updatedAt = record.updatedAt();
        deletedAt = record.deletedAt();
    }

    UUID id() {
        return UUID.fromString(id);
    }

    UUID userId() {
        return UUID.fromString(userId);
    }

    String name() {
        return name;
    }

    String visibility() {
        return visibility;
    }

    String items() {
        return items;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }

    Instant deletedAt() {
        return deletedAt;
    }

    long version() {
        return version;
    }
}
