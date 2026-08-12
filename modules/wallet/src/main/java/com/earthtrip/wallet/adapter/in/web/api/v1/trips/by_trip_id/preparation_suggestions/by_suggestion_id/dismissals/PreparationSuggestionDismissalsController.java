package com.earthtrip.wallet.adapter.in.web.api.v1.trips.by_trip_id.preparation_suggestions.by_suggestion_id.dismissals;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.PreparationSuggestionUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/preparation-suggestions/{suggestionId}/dismissals")
class PreparationSuggestionDismissalsController {

    private final PreparationSuggestionUseCase useCase;
    private final CurrentActor actor;

    PreparationSuggestionDismissalsController(
            PreparationSuggestionUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void post(
            @PathVariable UUID tripId,
            @PathVariable UUID suggestionId,
            @Valid @RequestBody PreparationSuggestionDismissalRequest request) {
        useCase.dismiss(tripId, suggestionId, actor.requireUserId(), request.reason());
    }
}

record PreparationSuggestionDismissalRequest(@Size(max = 500) String reason) {}
