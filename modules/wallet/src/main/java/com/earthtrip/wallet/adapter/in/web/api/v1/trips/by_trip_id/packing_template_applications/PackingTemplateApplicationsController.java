package com.earthtrip.wallet.adapter.in.web.api.v1.trips.by_trip_id.packing_template_applications;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.PackingTemplateUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/packing-template-applications")
class PackingTemplateApplicationsController {

    private final PackingTemplateUseCase useCase;
    private final CurrentActor actor;

    PackingTemplateApplicationsController(PackingTemplateUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PackingTemplateUseCase.ApplicationResult post(
        @PathVariable UUID tripId,
        @Valid @RequestBody PackingTemplateApplicationRequest request
    ) {
        return useCase.apply(
            tripId, actor.requireUserId(),
            new PackingTemplateUseCase.ApplicationCommand(
                request.requestId(), request.templateId(), request.visibility()
            )
        );
    }
}

record PackingTemplateApplicationRequest(
    @NotNull UUID requestId,
    @NotNull UUID templateId,
    String visibility
) { }
