package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.operations;

import com.earthtrip.planning.application.port.in.OfflineOperationUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/operations")
class OperationsController {

    private final OfflineOperationUseCase useCase;
    private final CurrentActor actor;

    OperationsController(OfflineOperationUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    OfflineOperationUseCase.BatchResult post(
        @PathVariable UUID tripId,
        @Valid @RequestBody OperationBatchRequest request
    ) {
        return useCase.execute(
            tripId, actor.requireUserId(),
            request.operations().stream().map(OperationRequest::toCommand).toList()
        );
    }
}

record OperationBatchRequest(
    @NotNull @Size(min = 1, max = 100) @Valid List<OperationRequest> operations
) { }

record OperationRequest(
    @NotNull UUID operationId,
    @NotBlank String action,
    @NotBlank String resourceType,
    @NotNull UUID resourceId,
    UUID parentId,
    LocalDate localDate,
    Map<String, Object> payload,
    String status,
    @PositiveOrZero Integer sortOrder,
    @PositiveOrZero long baseVersion,
    String stateType,
    Map<String, Object> stateValue
) {
    OfflineOperationUseCase.OperationCommand toCommand() {
        return new OfflineOperationUseCase.OperationCommand(
            operationId, action, resourceType, resourceId, parentId, localDate,
            payload, status, sortOrder, baseVersion, stateType, stateValue
        );
    }
}
