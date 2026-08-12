package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.days.by_day_id.schedule_items.by_item_id;

import com.earthtrip.planning.application.port.in.*;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/days/{dayId}/schedule-items/{itemId}")
class ScheduleItemByIdController {
    private final ScheduleUseCase useCase;
    private final CurrentActor actor;

    ScheduleItemByIdController(ScheduleUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    PlanningResourceUseCase.ResourceResult get(
            @PathVariable UUID tripId, @PathVariable UUID dayId, @PathVariable UUID itemId) {
        return useCase.get(tripId, dayId, itemId, actor.requireUserId());
    }

    @PatchMapping
    PlanningResourceUseCase.ResourceResult patch(
            @PathVariable UUID tripId,
            @PathVariable UUID dayId,
            @PathVariable UUID itemId,
            @Valid @RequestBody ScheduleMutation r) {
        return useCase.update(
                tripId,
                dayId,
                itemId,
                actor.requireUserId(),
                new PlanningResourceUseCase.ResourceCommand(
                        itemId,
                        dayId,
                        null,
                        r.payload(),
                        r.status(),
                        r.sortOrder(),
                        r.baseVersion()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @PathVariable UUID tripId,
            @PathVariable UUID dayId,
            @PathVariable UUID itemId,
            @Valid @RequestBody ScheduleDelete r) {
        useCase.delete(tripId, dayId, itemId, actor.requireUserId(), r.baseVersion());
    }
}

record ScheduleMutation(
        Map<String, Object> payload, String status, Integer sortOrder, @Min(0) long baseVersion) {}

record ScheduleDelete(@Min(0) long baseVersion) {}
