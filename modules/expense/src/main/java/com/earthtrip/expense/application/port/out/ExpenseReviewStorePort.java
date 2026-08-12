package com.earthtrip.expense.application.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseReviewStorePort {

    List<ReviewRecord> findAll(UUID tripId);

    Optional<ReviewRecord> find(UUID tripId, LocalDate localDate);

    ReviewRecord save(ReviewRecord record);

    void delete(UUID tripId, LocalDate localDate);

    record ReviewRecord(
            UUID tripId,
            LocalDate localDate,
            UUID completedBy,
            String note,
            Instant completedAt,
            long version) {}
}
