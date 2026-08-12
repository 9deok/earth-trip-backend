package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.research_source_batches;

import com.earthtrip.planning.application.port.in.PlanningCollectionUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/research-source-batches")
class ResearchSourceBatchesController {

    private final PlanningCollectionUseCase useCase;
    private final CurrentActor actor;

    ResearchSourceBatchesController(PlanningCollectionUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    PlanningCollectionUseCase.BatchResult create(
            @PathVariable UUID tripId, @Valid @RequestBody ResearchSourceBatchRequest request) {
        return useCase.createResearchSourceBatch(
                tripId,
                actor.requireUserId(),
                request.items().stream().map(ResearchSourceBatchItem::command).toList());
    }
}

record ResearchSourceBatchRequest(
        @NotEmpty @Size(max = 100) List<@Valid ResearchSourceBatchItem> items) {}

record ResearchSourceBatchItem(
        @NotNull UUID requestId,
        UUID categoryId,
        @NotNull Map<String, Object> payload,
        @PositiveOrZero Integer sortOrder) {
    PlanningCollectionUseCase.ResearchSourceItem command() {
        return new PlanningCollectionUseCase.ResearchSourceItem(
                requestId, categoryId, payload, sortOrder);
    }
}
