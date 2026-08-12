package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.research_sources.by_source_id;

import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/research-sources/{sourceId}")
class ResearchSourceByIdController {
    private final PlanningResourceUseCase useCase;
    private final CurrentActor actor;

    ResearchSourceByIdController(PlanningResourceUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    PlanningResourceUseCase.ResourceResult get(
            @PathVariable UUID tripId, @PathVariable UUID sourceId) {
        return useCase.get(tripId, actor.requireUserId(), "RESEARCH_SOURCE", sourceId);
    }

    @PatchMapping
    PlanningResourceUseCase.ResourceResult patch(
            @PathVariable UUID tripId,
            @PathVariable UUID sourceId,
            @Valid @RequestBody SourceMutation r) {
        return useCase.update(
                tripId,
                actor.requireUserId(),
                "RESEARCH_SOURCE",
                sourceId,
                PlanningResourceUseCase.WritePermission.EDITOR,
                new PlanningResourceUseCase.ResourceCommand(
                        sourceId,
                        r.categoryId(),
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
            @PathVariable UUID sourceId,
            @Valid @RequestBody SourceDelete r) {
        useCase.delete(
                tripId,
                actor.requireUserId(),
                "RESEARCH_SOURCE",
                sourceId,
                PlanningResourceUseCase.WritePermission.EDITOR,
                r.baseVersion());
    }
}

record SourceMutation(
        UUID categoryId,
        Map<String, Object> payload,
        String status,
        Integer sortOrder,
        @Min(0) long baseVersion) {}

record SourceDelete(@Min(0) long baseVersion) {}
