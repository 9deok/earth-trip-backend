package com.earthtrip.trip.adapter.in.web.api.v1.me.trip_templates.by_template_id;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.TripTemplateUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/trip-templates/{templateId}")
class TripTemplateByIdController {

    private final TripTemplateUseCase useCase;
    private final CurrentActor actor;

    TripTemplateByIdController(TripTemplateUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    TripTemplateUseCase.TemplateResult get(@PathVariable UUID templateId) {
        return useCase.get(templateId, actor.requireUserId());
    }

    @PatchMapping
    TripTemplateUseCase.TemplateResult update(
            @PathVariable UUID templateId, @Valid @RequestBody TripTemplateUpdateRequest request) {
        return useCase.update(
                templateId,
                actor.requireUserId(),
                new TripTemplateUseCase.UpdateCommand(
                        request.name(),
                        request.description(),
                        request.includeScopes(),
                        request.baseVersion()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID templateId, @RequestParam @PositiveOrZero long baseVersion) {
        useCase.delete(templateId, actor.requireUserId(), baseVersion);
    }
}

record TripTemplateUpdateRequest(
        @Size(min = 1, max = 120) String name,
        @Size(max = 500) String description,
        Set<String> includeScopes,
        @PositiveOrZero long baseVersion) {}
