package com.earthtrip.expense.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface StatementImportStorePort {

    List<ImportRecord> findImports(UUID tripId);

    Optional<ImportRecord> findImport(UUID importId);

    ImportRecord saveImport(ImportRecord record);

    List<CandidateRecord> findCandidates(UUID importId);

    Optional<CandidateRecord> findCandidate(UUID candidateId);

    CandidateRecord saveCandidate(CandidateRecord record);

    record ImportRecord(
        UUID id,
        UUID tripId,
        String source,
        String status,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        long version
    ) { }

    record CandidateRecord(
        UUID id,
        UUID importId,
        UUID tripId,
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
        long version
    ) { }
}
