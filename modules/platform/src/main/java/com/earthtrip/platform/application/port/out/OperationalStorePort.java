package com.earthtrip.platform.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface OperationalStorePort {

    Optional<WebhookReceiptRecord> webhookReceipt(String provider, String sourceEventId);

    WebhookReceiptRecord saveWebhookReceipt(WebhookReceiptRecord record);

    Optional<JobRecord> job(UUID id);

    List<JobRecord> jobs(String status, String jobType, int limit);

    JobRecord saveJob(JobRecord record);

    Optional<DeadLetterRecord> deadLetter(UUID id);

    Optional<DeadLetterRecord> openDeadLetterForJob(UUID jobId);

    List<DeadLetterRecord> deadLetters(String status, int limit);

    DeadLetterRecord saveDeadLetter(DeadLetterRecord record);

    AuditRecord saveAudit(AuditRecord record);

    List<AuditRecord> auditEvents(String action, String targetType, String targetId, int limit);

    record WebhookReceiptRecord(
            UUID id,
            String provider,
            String sourceEventId,
            String payloadDigest,
            UUID jobId,
            Instant receivedAt) {}

    record JobRecord(
            UUID id,
            String jobType,
            String sourceEventId,
            String status,
            Map<String, Object> payload,
            int attemptCount,
            Instant availableAt,
            Instant createdAt,
            Instant updatedAt,
            Instant completedAt,
            String errorCode,
            String errorMessage,
            long version) {}

    record DeadLetterRecord(
            UUID id,
            UUID jobId,
            String eventType,
            Map<String, Object> payload,
            String errorCode,
            String errorMessage,
            String status,
            Instant createdAt,
            Instant replayedAt,
            long version) {}

    record AuditRecord(
            Long sequenceId,
            UUID eventId,
            String actorType,
            String actorId,
            String action,
            String targetType,
            String targetId,
            String outcome,
            Map<String, Object> metadata,
            Instant occurredAt) {}
}
