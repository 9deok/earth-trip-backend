package com.earthtrip.trip.adapter.out.persistence.template;

import com.earthtrip.trip.application.port.out.TripTemplateStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trip_template_drafts")
class TripTemplateDraftJpaEntity {

    @Id @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;
    @Column(name = "template_id", nullable = false, length = 36)
    private String templateId;
    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;
    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TripTemplateDraftJpaEntity() { }

    TripTemplateDraftJpaEntity(TripTemplateStorePort.DraftRecord record) {
        requestId = record.requestId().toString();
        templateId = record.templateId().toString();
        tripId = record.tripId().toString();
        createdBy = record.createdBy().toString();
        createdAt = record.createdAt();
    }

    TripTemplateStorePort.DraftRecord toRecord() {
        return new TripTemplateStorePort.DraftRecord(
            UUID.fromString(requestId), UUID.fromString(templateId), UUID.fromString(tripId),
            UUID.fromString(createdBy), createdAt
        );
    }
}
