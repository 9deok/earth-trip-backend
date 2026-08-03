package com.earthtrip.platform.adapter.out.persistence.analysis;

import com.earthtrip.platform.application.port.out.AnalysisJobStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "analysis_jobs")
class AnalysisJobJpaEntity {

    @Id @Column(name = "id", nullable = false, length = 36)
    private String id;
    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;
    @Column(name = "target_type", nullable = false, length = 40)
    private String targetType;
    @Column(name = "target_id", nullable = false, length = 36)
    private String targetId;
    @Column(name = "input_payload", nullable = false, columnDefinition = "JSON")
    private String inputPayload;
    @Column(name = "suggestions", nullable = false, columnDefinition = "JSON")
    private String suggestions;
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    @Column(name = "confirmation_request_id", length = 36)
    private String confirmationRequestId;
    @Column(name = "confirmed_payload", columnDefinition = "JSON")
    private String confirmedPayload;
    @Column(name = "failure_code", length = 80)
    private String failureCode;
    @Column(name = "failure_message", length = 500)
    private String failureMessage;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version @Column(name = "version", nullable = false)
    private long version;

    protected AnalysisJobJpaEntity() { }

    AnalysisJobJpaEntity(AnalysisJobStorePort.JobRecord record, Serialized serialized) {
        id = record.id().toString();
        tripId = record.tripId().toString();
        targetType = record.targetType();
        targetId = record.targetId().toString();
        createdBy = record.createdBy().toString();
        createdAt = record.createdAt();
        apply(record, serialized);
    }

    void apply(AnalysisJobStorePort.JobRecord record, Serialized serialized) {
        inputPayload = serialized.inputPayload();
        suggestions = serialized.suggestions();
        status = record.status();
        confirmationRequestId = record.confirmationRequestId() == null
            ? null : record.confirmationRequestId().toString();
        confirmedPayload = serialized.confirmedPayload();
        failureCode = record.failureCode();
        failureMessage = record.failureMessage();
        attemptCount = record.attemptCount();
        updatedAt = record.updatedAt();
    }

    String inputPayload() { return inputPayload; }
    String suggestions() { return suggestions; }
    String confirmedPayload() { return confirmedPayload; }

    AnalysisJobStorePort.JobRecord toRecord(
        Map<String, Object> input,
        List<Map<String, Object>> suggestionList,
        Map<String, Object> confirmed
    ) {
        return new AnalysisJobStorePort.JobRecord(
            UUID.fromString(id), UUID.fromString(tripId), targetType,
            UUID.fromString(targetId), input, suggestionList, status,
            confirmationRequestId == null ? null : UUID.fromString(confirmationRequestId),
            confirmed, failureCode, failureMessage, attemptCount,
            UUID.fromString(createdBy), createdAt, updatedAt, version
        );
    }

    record Serialized(String inputPayload, String suggestions, String confirmedPayload) { }
}
