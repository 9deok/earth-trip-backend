package com.earthtrip.planning.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TodayExecutionUseCase {

    TodayResult today(UUID tripId, UUID actorUserId);

    record TodayResult(
            String state,
            LocalDate localDate,
            UUID dayId,
            String timeZone,
            List<PlanningResourceUseCase.ResourceResult> items,
            int completedCount,
            int totalCount) {}
}
