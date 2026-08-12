package com.earthtrip.wallet.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ReservationImportUseCase {

    ImportResult create(UUID tripId, UUID actorUserId, ImportCommand command);

    ImportResult get(UUID jobId, UUID actorUserId);

    List<CandidateResult> candidates(UUID jobId, UUID actorUserId);

    ConfirmationResult confirm(UUID jobId, UUID actorUserId, List<ConfirmationItem> items);

    ImportResult dismiss(UUID jobId, UUID actorUserId, List<DismissalItem> items);

    ImportResult retry(UUID jobId, UUID actorUserId, long baseVersion);

    ImportResult cancel(UUID jobId, UUID actorUserId, long baseVersion);

    record ImportCommand(
            UUID requestId,
            String sourceType,
            Map<String, Object> sourcePayload,
            List<CandidateCommand> candidates) {}

    record CandidateCommand(
            UUID candidateId,
            String title,
            String candidateType,
            Map<String, Object> payload,
            BigDecimal confidence) {}

    record ConfirmationItem(
            UUID candidateId,
            UUID reservationRequestId,
            Map<String, Object> payloadOverride,
            String visibility,
            Integer sortOrder,
            long baseVersion) {}

    record DismissalItem(UUID candidateId, String reason, long baseVersion) {}

    record ImportResult(
            UUID jobId,
            UUID tripId,
            String sourceType,
            Map<String, Object> sourcePayload,
            String status,
            String failureCode,
            String failureMessage,
            int attemptCount,
            int candidateCount,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt,
            long version) {}

    record CandidateResult(
            UUID candidateId,
            UUID jobId,
            String title,
            String candidateType,
            Map<String, Object> payload,
            BigDecimal confidence,
            String status,
            UUID reservationId,
            String dismissalReason,
            Instant createdAt,
            Instant updatedAt,
            long version) {}

    record ConfirmationResult(
            UUID jobId, List<WalletRecordUseCase.RecordResult> reservations, String jobStatus) {}
}
