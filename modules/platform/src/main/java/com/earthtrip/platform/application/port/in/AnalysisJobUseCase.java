package com.earthtrip.platform.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AnalysisJobUseCase {

    JobResult createResearchSourceJob(
            UUID tripId, UUID sourceId, UUID actorUserId, CreateCommand command);

    JobResult createReceiptJob(
            UUID tripId, UUID expenseId, UUID actorUserId, CreateCommand command);

    JobResult get(UUID jobId, UUID actorUserId);

    List<SuggestionResult> suggestions(UUID jobId, UUID actorUserId);

    ConfirmationResult confirm(UUID jobId, UUID actorUserId, ConfirmationCommand command);

    JobResult retry(UUID jobId, UUID actorUserId, long baseVersion);

    JobResult cancel(UUID jobId, UUID actorUserId, long baseVersion);

    record CreateCommand(
            UUID requestId,
            Map<String, Object> inputPayload,
            List<SuggestionCommand> suggestions) {}

    record SuggestionCommand(
            String field,
            Object value,
            Double confidence,
            String sourceReference,
            List<String> warnings) {}

    record ConfirmationCommand(
            UUID requestId,
            Map<String, Object> confirmedFields,
            long targetBaseVersion,
            long jobBaseVersion) {}

    record JobResult(
            UUID jobId,
            UUID tripId,
            String targetType,
            UUID targetId,
            String status,
            int suggestionCount,
            String failureCode,
            String failureMessage,
            int attemptCount,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt,
            long version) {}

    record SuggestionResult(
            String field,
            Object value,
            Double confidence,
            String sourceReference,
            List<String> warnings) {}

    record ConfirmationResult(
            UUID jobId,
            String targetType,
            UUID targetId,
            Map<String, Object> confirmedFields,
            long targetVersion,
            String status) {}
}
