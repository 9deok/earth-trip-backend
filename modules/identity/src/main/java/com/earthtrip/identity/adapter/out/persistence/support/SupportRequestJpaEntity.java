package com.earthtrip.identity.adapter.out.persistence.support;

import com.earthtrip.identity.application.port.out.PersonalSupportStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "support_requests")
class SupportRequestJpaEntity {

    @Id @Column(name = "id", nullable = false, length = 36)
    private String id;
    @Column(name = "user_id", length = 36)
    private String userId;
    @Column(name = "category", nullable = false, length = 40)
    private String category;
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;
    @Column(name = "trace_id", length = 100)
    private String traceId;
    @Column(name = "diagnostics", columnDefinition = "TEXT")
    private String diagnostics;
    @Column(name = "status", nullable = false, length = 30)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SupportRequestJpaEntity() { }

    SupportRequestJpaEntity(PersonalSupportStorePort.SupportRecord record) {
        id = record.id().toString();
        userId = record.userId() == null ? null : record.userId().toString();
        category = record.category();
        description = record.description();
        traceId = record.traceId();
        diagnostics = record.diagnosticsJson();
        status = record.status();
        createdAt = record.createdAt();
    }

    PersonalSupportStorePort.SupportRecord toRecord() {
        return new PersonalSupportStorePort.SupportRecord(
            UUID.fromString(id), userId == null ? null : UUID.fromString(userId),
            category, description, traceId, diagnostics, status, createdAt
        );
    }
}
