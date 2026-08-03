package com.earthtrip.trip.adapter.in.web.api.v1.trip_templates.by_template_id.drafts;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.TripManagementUseCase;
import com.earthtrip.trip.application.port.in.TripTemplateUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trip-templates/{templateId}/drafts")
class TripTemplateDraftsController {

    private final TripTemplateUseCase useCase;
    private final CurrentActor actor;

    TripTemplateDraftsController(TripTemplateUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TripManagementUseCase.TripResult create(
        @PathVariable UUID templateId,
        @Valid @RequestBody TripTemplateDraftRequest request
    ) {
        return useCase.createDraft(
            templateId, actor.requireUserId(),
            new TripTemplateUseCase.DraftCommand(
                request.requestId(), request.title(), request.startDate(),
                request.timeZone(), request.defaultCurrency()
            )
        );
    }
}

record TripTemplateDraftRequest(
    @NotNull UUID requestId,
    @Size(min = 1, max = 120) String title,
    LocalDate startDate,
    String timeZone,
    String defaultCurrency
) { }
