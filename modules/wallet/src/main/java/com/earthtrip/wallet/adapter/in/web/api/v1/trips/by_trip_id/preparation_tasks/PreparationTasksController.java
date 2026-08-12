package com.earthtrip.wallet.adapter.in.web.api.v1.trips.by_trip_id.preparation_tasks;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.WalletRecordUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/preparation-tasks")
class PreparationTasksController {
    private final WalletRecordUseCase useCase;
    private final CurrentActor actor;

    PreparationTasksController(WalletRecordUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    List<WalletRecordUseCase.RecordResult> get(@PathVariable UUID tripId) {
        return useCase.list(tripId, actor.requireUserId(), "PREPARATION_TASK", null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    WalletRecordUseCase.RecordResult post(
            @PathVariable UUID tripId, @Valid @RequestBody TaskMutation r) {
        return useCase.create(
                tripId,
                actor.requireUserId(),
                "PREPARATION_TASK",
                false,
                new WalletRecordUseCase.Command(
                        r.requestId(),
                        null,
                        r.payload(),
                        "OPEN",
                        r.visibility(),
                        r.sortOrder(),
                        0));
    }
}

record TaskMutation(
        @NotNull UUID requestId,
        @NotNull Map<String, Object> payload,
        String visibility,
        Integer sortOrder) {}
