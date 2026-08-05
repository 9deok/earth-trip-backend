package com.earthtrip.platform.adapter.out.persistence.operation;

import com.earthtrip.platform.application.port.out.OperationalStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "operational_jobs")
class OperationalJobJpaEntity {

    @Id @Column(name = "id", nullable = false, length = 36)
    private String id;
    @Column(name = "job_type", nullable = false, length = 50)
    private String jobType;
    @Column(name = "source_event_id", length = 160)
    private String sourceEventId;
    @Column(name = "status", nullable = false, length = 30)
    private String status;
    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "available_at", nullable = false)
    private Instant availableAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "error_code", length = 80)
    private String errorCode;
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    @Version @Column(name = "version", nullable = false)
    private long version;

    protected OperationalJobJpaEntity() { }

    OperationalJobJpaEntity(OperationalStorePort.JobRecord record, String payload) {
        id = record.id().toString();
        jobType = record.jobType();
        sourceEventId = record.sourceEventId();
        createdAt = record.createdAt();
        apply(record, payload);
    }

    void apply(OperationalStorePort.JobRecord record, String payload) {
        status = record.status();
        this.payload = payload;
        attemptCount = record.attemptCount();
        availableAt = record.availableAt();
        updatedAt = record.updatedAt();
        completedAt = record.completedAt();
        errorCode = record.errorCode();
        errorMessage = record.errorMessage();
    }

    String payload() {
        return payload;
    }

    String id() {
        return id;
    }

    String jobType() {
        return jobType;
    }

    OperationalStorePort.JobRecord record(java.util.Map<String, Object> payloadValue) {
        return new OperationalStorePort.JobRecord(
            java.util.UUID.fromString(id),
            jobType,
            sourceEventId,
            status,
            payloadValue,
            attemptCount,
            availableAt,
            createdAt,
            updatedAt,
            completedAt,
            errorCode,
            errorMessage,
            version
        );
    }
}
