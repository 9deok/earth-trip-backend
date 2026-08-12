package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.days.by_day_id.schedule_item_order;

import com.earthtrip.planning.application.port.in.*;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/days/{dayId}/schedule-item-order")
class ScheduleItemOrderController {
    private final ScheduleUseCase useCase;
    private final CurrentActor actor;

    ScheduleItemOrderController(ScheduleUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @PutMapping
    List<PlanningResourceUseCase.ResourceResult> put(
            @PathVariable UUID tripId,
            @PathVariable UUID dayId,
            @Valid @RequestBody OrderMutation r) {
        return useCase.reorder(
                tripId,
                dayId,
                actor.requireUserId(),
                r.items().stream()
                        .map(
                                i ->
                                        new ScheduleUseCase.OrderItem(
                                                i.itemId(), i.sortOrder(), i.baseVersion()))
                        .toList());
    }
}

record OrderMutation(@NotEmpty List<@Valid OrderItemMutation> items) {}

record OrderItemMutation(@NotNull UUID itemId, @Min(0) int sortOrder, @Min(0) long baseVersion) {}
