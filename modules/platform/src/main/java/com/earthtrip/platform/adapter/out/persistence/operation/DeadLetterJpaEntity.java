package com.earthtrip.platform.adapter.out.persistence.operation;

import com.earthtrip.platform.application.port.out.OperationalStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "dead_letter_events")
class DeadLetterJpaEntity {

    @Id @Column(name = "id", nullable = false, length = 36)
    private String id;
    @Column(name = "job_id", nullable = false, length = 36)
    private String jobId;
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;
    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;
    @Column(name = "error_code", nullable = false, length = 80)
    private String errorCode;
    @Column(name = "error_message", nullable = false, length = 500)
    private String errorMessage;
    @Column(name = "status", nullable = false, length = 30)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "replayed_at")
    private Instant replayedAt;
    @Version @Column(name = "version", nullable = false)
    private long version;

    protected DeadLetterJpaEntity() { }

    DeadLetterJpaEntity(OperationalStorePort.DeadLetterRecord record, String payload) {
        id = record.id().toString();
        jobId = record.jobId().toString();
        eventType = record.eventType();
        createdAt = record.createdAt();
        apply(record, payload);
    }

    void apply(OperationalStorePort.DeadLetterRecord record, String payload) {
        this.payload = payload;
        errorCode = record.errorCode();
        errorMessage = record.errorMessage();
        status = record.status();
        replayedAt = record.replayedAt();
    }

    String payload() {
        return payload;
    }

    OperationalStorePort.DeadLetterRecord record(Map<String, Object> payloadValue) {
        return new OperationalStorePort.DeadLetterRecord(
            UUID.fromString(id),
            UUID.fromString(jobId),
            eventType,
            payloadValue,
            errorCode,
            errorMessage,
            status,
            createdAt,
            replayedAt,
            version
        );
    }
}
