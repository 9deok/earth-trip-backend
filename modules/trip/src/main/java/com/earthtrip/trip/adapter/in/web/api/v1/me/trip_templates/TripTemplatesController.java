package com.earthtrip.trip.adapter.in.web.api.v1.me.trip_templates;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.TripTemplateUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/trip-templates")
class TripTemplatesController {

    private final TripTemplateUseCase useCase;
    private final CurrentActor actor;

    TripTemplatesController(TripTemplateUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<TripTemplateUseCase.TemplateResult> list() {
        return useCase.list(actor.requireUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TripTemplateUseCase.TemplateResult create(
        @Valid @RequestBody TripTemplateCreateRequest request
    ) {
        return useCase.create(
            actor.requireUserId(),
            new TripTemplateUseCase.CreateCommand(
                request.requestId(), request.sourceTripId(), request.name(),
                request.description(), request.includeScopes()
            )
        );
    }
}

record TripTemplateCreateRequest(
    @NotNull UUID requestId,
    @NotNull UUID sourceTripId,
    @NotBlank @Size(max = 120) String name,
    @Size(max = 500) String description,
    Set<String> includeScopes
) { }
