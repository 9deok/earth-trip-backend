package com.earthtrip.planning.application.service.view;

import com.earthtrip.planning.api.TripPlanningView;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.planning.application.port.in.TodayExecutionUseCase;
import com.earthtrip.planning.application.port.in.TripDayUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class TripPlanningViewService implements TripPlanningView {

    private final TripDayUseCase days;
    private final TodayExecutionUseCase execution;
    private final PlanningResourceUseCase resources;

    TripPlanningViewService(
            TripDayUseCase days,
            TodayExecutionUseCase execution,
            PlanningResourceUseCase resources) {
        this.days = days;
        this.execution = execution;
        this.resources = resources;
    }

    @Override
    public PlanningSnapshot snapshot(UUID tripId, UUID actorUserId) {
        TodayExecutionUseCase.TodayResult today = execution.today(tripId, actorUserId);
        return new PlanningSnapshot(
                days.list(tripId, actorUserId).stream()
                        .map(
                                day ->
                                        new Day(
                                                day.dayId(),
                                                day.localDate(),
                                                day.dayNumber(),
                                                day.timeZone()))
                        .toList(),
                new Today(
                        today.state(),
                        today.localDate(),
                        today.dayId(),
                        today.timeZone(),
                        today.items().stream()
                                .map(
                                        item ->
                                                new ScheduleItem(
                                                        item.resourceId(),
                                                        item.payload(),
                                                        item.status(),
                                                        item.sortOrder(),
                                                        item.version()))
                                .toList(),
                        today.completedCount(),
                        today.totalCount()));
    }

    @Override
    public NextDecision nextDecision(UUID tripId, UUID actorUserId) {
        return resources.list(tripId, actorUserId, "PLACE_CANDIDATE", null, null).stream()
                .filter(resource -> "DISCUSSING".equals(resource.status()))
                .sorted((left, right) -> Integer.compare(left.sortOrder(), right.sortOrder()))
                .map(
                        resource ->
                                new NextDecision(
                                        resource.resourceId(),
                                        String.valueOf(
                                                resource.payload()
                                                        .getOrDefault("title", "함께 결정할 후보")),
                                        voteCount(resource.userStates(), "WANT"),
                                        voteCount(resource.userStates(), "HOLD"),
                                        voteCount(resource.userStates(), "EXCLUDE"),
                                        resource.version()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<SearchEntry> searchEntries(UUID tripId, UUID actorUserId) {
        return resources.listAll(tripId, actorUserId).stream()
                .map(
                        resource ->
                                new SearchEntry(
                                        resource.resourceId(),
                                        resource.resourceType(),
                                        resource.parentId(),
                                        resource.localDate(),
                                        resource.sortOrder(),
                                        resource.payload(),
                                        resource.status()))
                .toList();
    }

    private static int voteCount(
            List<PlanningResourceUseCase.UserStateResult> states, String choice) {
        return (int)
                states.stream()
                        .filter(state -> state.stateType().equals("PREFERENCE"))
                        .map(PlanningResourceUseCase.UserStateResult::value)
                        .map(value -> value.get("choice"))
                        .filter(
                                value ->
                                        value != null
                                                && choice.equalsIgnoreCase(String.valueOf(value)))
                        .count();
    }
}
