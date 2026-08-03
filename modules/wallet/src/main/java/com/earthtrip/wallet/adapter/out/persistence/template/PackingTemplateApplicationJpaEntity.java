package com.earthtrip.wallet.adapter.out.persistence.template;

import com.earthtrip.wallet.application.port.out.PackingTemplateStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "packing_template_applications")
class PackingTemplateApplicationJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "template_id", nullable = false, length = 36)
    private String templateId;

    @Column(name = "applied_by", nullable = false, length = 36)
    private String appliedBy;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "item_ids", nullable = false, columnDefinition = "JSON")
    private String itemIds;

    protected PackingTemplateApplicationJpaEntity() { }

    PackingTemplateApplicationJpaEntity(
        PackingTemplateStorePort.ApplicationRecord record,
        String itemIds
    ) {
        id = record.id().toString();
        tripId = record.tripId().toString();
        templateId = record.templateId().toString();
        appliedBy = record.appliedBy().toString();
        appliedAt = record.appliedAt();
        this.itemIds = itemIds;
    }

    UUID id() { return UUID.fromString(id); }

    UUID tripId() { return UUID.fromString(tripId); }

    UUID templateId() { return UUID.fromString(templateId); }

    UUID appliedBy() { return UUID.fromString(appliedBy); }

    Instant appliedAt() { return appliedAt; }

    String itemIds() { return itemIds; }
}
