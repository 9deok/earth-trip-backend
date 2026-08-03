package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.schedule_item_moves;

import com.earthtrip.planning.application.port.in.ScheduleMoveUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/schedule-item-moves")
class ScheduleItemMovesController {

    private final ScheduleMoveUseCase useCase;
    private final CurrentActor actor;

    ScheduleItemMovesController(ScheduleMoveUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    ScheduleMoveUseCase.MoveResult post(
        @PathVariable UUID tripId,
        @Valid @RequestBody ScheduleItemMoveRequest request
    ) {
        return useCase.move(
            tripId,
            actor.requireUserId(),
            new ScheduleMoveUseCase.MoveCommand(
                request.itemId(), request.sourceDayId(), request.targetDayId(),
                request.itemBaseVersion(), order(request.sourceOrder()),
                order(request.targetOrder())
            )
        );
    }

    private static List<ScheduleMoveUseCase.OrderItem> order(List<ScheduleOrderRequest> requests) {
        return requests == null
            ? null
            : requests.stream()
                .map(item -> new ScheduleMoveUseCase.OrderItem(
                    item.itemId(), item.sortOrder(), item.baseVersion()
                ))
                .toList();
    }
}

record ScheduleItemMoveRequest(
    @NotNull UUID itemId,
    @NotNull UUID sourceDayId,
    @NotNull UUID targetDayId,
    @PositiveOrZero long itemBaseVersion,
    @Valid List<ScheduleOrderRequest> sourceOrder,
    @NotNull @Valid List<ScheduleOrderRequest> targetOrder
) { }

record ScheduleOrderRequest(
    @NotNull UUID itemId,
    @PositiveOrZero int sortOrder,
    @PositiveOrZero long baseVersion
) { }
