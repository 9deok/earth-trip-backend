package com.earthtrip.planning.application.service.execution;

import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.planning.application.port.in.ScheduleUseCase;
import com.earthtrip.planning.application.port.in.TodayExecutionUseCase;
import com.earthtrip.planning.application.port.in.TripDayUseCase;
import com.earthtrip.trip.api.TripAccess;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class TodayExecutionService implements TodayExecutionUseCase {

    private final TripAccess access;
    private final TripDayUseCase days;
    private final ScheduleUseCase schedule;
    private final Clock clock;

    TodayExecutionService(
            TripAccess access, TripDayUseCase days, ScheduleUseCase schedule, Clock clock) {
        this.access = access;
        this.days = days;
        this.schedule = schedule;
        this.clock = clock;
    }

    @Override
    public TodayResult today(UUID tripId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        TripAccess.PublicTripResult info = access.publicInfo(tripId);
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(info.timeZone())));
        TripDayUseCase.DayResult day =
                days.list(tripId, actorUserId).stream()
                        .filter(candidate -> candidate.localDate().equals(today))
                        .findFirst()
                        .orElse(null);
        if (day == null) {
            String state =
                    info.startDate() == null || today.isBefore(info.startDate())
                            ? "NOT_STARTED"
                            : "ENDED";
            return new TodayResult(state, today, null, info.timeZone(), List.of(), 0, 0);
        }
        List<PlanningResourceUseCase.ResourceResult> items =
                schedule.list(tripId, day.dayId(), actorUserId);
        int completedCount =
                (int)
                        items.stream()
                                .filter(
                                        item ->
                                                Set.of("COMPLETED", "SKIPPED")
                                                        .contains(item.status()))
                                .count();
        return new TodayResult(
                "TRAVELING",
                today,
                day.dayId(),
                info.timeZone(),
                items,
                completedCount,
                items.size());
    }
}
