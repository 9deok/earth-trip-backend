package com.earthtrip.planning.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExecutionSummaryUseCase {

    SummaryResult get(UUID tripId, UUID actorUserId);

    record SummaryResult(
        int totalCount,
        int completedCount,
        int skippedCount,
        int delayedCount,
        List<DaySummary> days
    ) { }

    record DaySummary(
        UUID dayId,
        LocalDate localDate,
        int totalCount,
        int completedCount,
        int skippedCount,
        int delayedCount
    ) { }
}
