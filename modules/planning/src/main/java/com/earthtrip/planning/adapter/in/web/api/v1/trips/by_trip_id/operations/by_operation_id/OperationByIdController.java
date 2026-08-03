package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.operations.by_operation_id;

import com.earthtrip.planning.application.port.in.OfflineOperationUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/operations/{operationId}")
class OperationByIdController {

    private final OfflineOperationUseCase useCase;
    private final CurrentActor actor;

    OperationByIdController(OfflineOperationUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    OfflineOperationUseCase.OperationResult get(
        @PathVariable UUID tripId,
        @PathVariable UUID operationId
    ) {
        return useCase.get(tripId, operationId, actor.requireUserId());
    }
}
