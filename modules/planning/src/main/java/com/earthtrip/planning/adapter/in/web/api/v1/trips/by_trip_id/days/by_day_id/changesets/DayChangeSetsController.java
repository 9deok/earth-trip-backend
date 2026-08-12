package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.days.by_day_id.changesets;

import com.earthtrip.planning.application.port.in.DayChangeSetUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/days/{dayId}/changesets")
class DayChangeSetsController {

    private final DayChangeSetUseCase useCase;
    private final CurrentActor actor;

    DayChangeSetsController(DayChangeSetUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    DayChangeSetUseCase.ChangeSetResult post(
            @PathVariable UUID tripId,
            @PathVariable UUID dayId,
            @Valid @RequestBody DayChangeSetRequest request) {
        return useCase.apply(tripId, dayId, actor.requireUserId(), request.toCommand());
    }
}

record DayChangeSetRequest(
        @NotNull UUID requestId, @NotNull @Valid List<DayChangeSetOrderRequest> order) {
    DayChangeSetUseCase.ChangeSetCommand toCommand() {
        return new DayChangeSetUseCase.ChangeSetCommand(
                requestId, order.stream().map(DayChangeSetOrderRequest::toItem).toList());
    }
}

record DayChangeSetOrderRequest(
        @NotNull UUID itemId, @PositiveOrZero int sortOrder, @PositiveOrZero long baseVersion) {
    DayChangeSetUseCase.OrderItem toItem() {
        return new DayChangeSetUseCase.OrderItem(itemId, sortOrder, baseVersion);
    }
}
