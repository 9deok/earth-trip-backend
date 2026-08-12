package com.earthtrip.wallet.adapter.in.web.api.v1.trips.by_trip_id.preparation_tasks.by_task_id;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.WalletRecordUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/preparation-tasks/{taskId}")
class PreparationTaskByIdController {
    private final WalletRecordUseCase useCase;
    private final CurrentActor actor;

    PreparationTaskByIdController(WalletRecordUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @PatchMapping
    WalletRecordUseCase.RecordResult patch(
            @PathVariable UUID tripId,
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskMutation r) {
        return useCase.update(
                tripId,
                actor.requireUserId(),
                "PREPARATION_TASK",
                taskId,
                false,
                new WalletRecordUseCase.Command(
                        taskId,
                        null,
                        r.payload(),
                        r.status(),
                        r.visibility(),
                        r.sortOrder(),
                        r.baseVersion()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @PathVariable UUID tripId,
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskDelete r) {
        useCase.delete(
                tripId, actor.requireUserId(), "PREPARATION_TASK", taskId, false, r.baseVersion());
    }
}

record TaskMutation(
        Map<String, Object> payload,
        String status,
        String visibility,
        Integer sortOrder,
        @Min(0) long baseVersion) {}

record TaskDelete(@Min(0) long baseVersion) {}
