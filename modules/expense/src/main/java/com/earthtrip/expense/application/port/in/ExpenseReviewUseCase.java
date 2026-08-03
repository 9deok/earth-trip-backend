package com.earthtrip.expense.application.port.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExpenseReviewUseCase {

    List<ReviewDayResult> list(UUID tripId, UUID actorUserId);

    ReviewDayResult complete(
        UUID tripId,
        LocalDate localDate,
        UUID actorUserId,
        String note,
        long baseVersion
    );

    void reopen(
        UUID tripId,
        LocalDate localDate,
        UUID actorUserId,
        long baseVersion
    );

    record ReviewDayResult(
        LocalDate localDate,
        UUID completedBy,
        String note,
        Instant completedAt,
        long version
    ) { }
}
