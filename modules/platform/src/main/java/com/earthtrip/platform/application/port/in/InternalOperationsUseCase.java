package com.earthtrip.platform.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface InternalOperationsUseCase {

    WebhookResult acceptWebhook(
        String provider,
        String eventId,
        String timestamp,
        String signature,
        String rawBody
    );

    List<JobResult> jobs(String status, String jobType, int limit);

    JobResult retryJob(UUID jobId);

    List<DeadLetterResult> deadLetters(String status, int limit);

    JobResult replayDeadLetter(UUID eventId);

    List<AuditResult> auditEvents(
        String action,
        String targetType,
        String targetId,
        int limit
    );

    record WebhookResult(
        UUID jobId,
        String provider,
        String sourceEventId,
        String status,
        boolean duplicate,
        Instant receivedAt
    ) { }

    record JobResult(
        UUID jobId,
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
        long version
    ) { }

    record DeadLetterResult(
        UUID eventId,
        UUID jobId,
        String eventType,
        Map<String, Object> payload,
        String errorCode,
        String errorMessage,
        String status,
        Instant createdAt,
        Instant replayedAt,
        long version
    ) { }

    record AuditResult(
        long sequenceId,
        UUID eventId,
        String actorType,
        String actorId,
        String action,
        String targetType,
        String targetId,
        String outcome,
        Map<String, Object> metadata,
        Instant occurredAt
    ) { }
}
