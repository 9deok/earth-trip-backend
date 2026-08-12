package com.earthtrip.expense.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface StatementImportUseCase {

    List<ImportResult> list(UUID tripId, UUID actorUserId);

    ImportResult create(UUID tripId, UUID actorUserId, ImportCommand command);

    List<CandidateResult> candidates(UUID tripId, UUID importId, UUID actorUserId);

    CandidateResult updateCandidate(
            UUID tripId,
            UUID importId,
            UUID candidateId,
            UUID actorUserId,
            CandidateUpdate command);

    CandidateResult linkExpense(
            UUID tripId,
            UUID importId,
            UUID candidateId,
            UUID actorUserId,
            UUID expenseId,
            long baseVersion);

    CandidateResult unlinkExpense(
            UUID tripId, UUID importId, UUID candidateId, UUID actorUserId, long baseVersion);

    CandidateResult dismiss(
            UUID tripId,
            UUID importId,
            UUID candidateId,
            UUID actorUserId,
            String reason,
            long baseVersion);

    ConfirmationResult confirm(
            UUID tripId, UUID importId, UUID actorUserId, List<ConfirmationItem> items);

    record ImportCommand(UUID requestId, String source, List<CandidateCommand> candidates) {}

    record CandidateCommand(
            UUID candidateId,
            String title,
            long amountMinor,
            String currency,
            Instant occurredAt,
            UUID payerUserId,
            Map<String, Object> payload) {}

    record CandidateUpdate(
            String title,
            Long amountMinor,
            String currency,
            Instant occurredAt,
            UUID payerUserId,
            Map<String, Object> payload,
            long baseVersion) {}

    record ConfirmationItem(
            UUID candidateId,
            UUID expenseRequestId,
            String categoryCode,
            Map<UUID, Long> participantShares,
            String visibility,
            long baseVersion) {}

    record ImportResult(
            UUID importId,
            String source,
            String status,
            int candidateCount,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt,
            long version) {}

    record CandidateResult(
            UUID candidateId,
            UUID importId,
            String title,
            long amountMinor,
            String currency,
            Instant occurredAt,
            UUID payerUserId,
            Map<String, Object> payload,
            String status,
            UUID expenseId,
            Instant createdAt,
            Instant updatedAt,
            long version) {}

    record ConfirmationResult(
            UUID importId, List<ExpenseUseCase.ExpenseResult> expenses, String importStatus) {}
}
