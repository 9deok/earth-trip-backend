package com.earthtrip.planning.application.service.execution;

import com.earthtrip.planning.application.port.in.ExecutionSummaryUseCase;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.planning.application.port.in.ScheduleUseCase;
import com.earthtrip.planning.application.port.in.TripDayUseCase;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ExecutionSummaryService implements ExecutionSummaryUseCase {

    private final TripDayUseCase days;
    private final ScheduleUseCase schedule;

    ExecutionSummaryService(TripDayUseCase days, ScheduleUseCase schedule) {
        this.days = days;
        this.schedule = schedule;
    }

    @Override
    public SummaryResult get(UUID tripId, UUID actorUserId) {
        List<DaySummary> summaries = new ArrayList<>();
        for (TripDayUseCase.DayResult day : days.list(tripId, actorUserId)) {
            List<PlanningResourceUseCase.ResourceResult> items =
                    schedule.list(tripId, day.dayId(), actorUserId);
            int completed = count(items, "COMPLETED");
            int skipped = count(items, "SKIPPED");
            int delayed = (int) items.stream().filter(ExecutionSummaryService::delayed).count();
            summaries.add(
                    new DaySummary(
                            day.dayId(),
                            day.localDate(),
                            items.size(),
                            completed,
                            skipped,
                            delayed));
        }
        return new SummaryResult(
                summaries.stream().mapToInt(DaySummary::totalCount).sum(),
                summaries.stream().mapToInt(DaySummary::completedCount).sum(),
                summaries.stream().mapToInt(DaySummary::skippedCount).sum(),
                summaries.stream().mapToInt(DaySummary::delayedCount).sum(),
                List.copyOf(summaries));
    }

    private static int count(List<PlanningResourceUseCase.ResourceResult> items, String status) {
        return (int) items.stream().filter(item -> item.status().equals(status)).count();
    }

    private static boolean delayed(PlanningResourceUseCase.ResourceResult item) {
        if (item.status().equals("DELAYED")) {
            return true;
        }
        Object value = item.payload().get("delayMinutes");
        return value instanceof Number number && number.intValue() > 0;
    }
}
