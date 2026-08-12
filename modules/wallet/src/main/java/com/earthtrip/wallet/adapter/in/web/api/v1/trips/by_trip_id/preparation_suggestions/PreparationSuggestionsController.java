package com.earthtrip.wallet.adapter.in.web.api.v1.trips.by_trip_id.preparation_suggestions;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.PreparationSuggestionUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/preparation-suggestions")
class PreparationSuggestionsController {

    private final PreparationSuggestionUseCase useCase;
    private final CurrentActor actor;

    PreparationSuggestionsController(PreparationSuggestionUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<PreparationSuggestionUseCase.SuggestionResult> get(@PathVariable UUID tripId) {
        return useCase.list(tripId, actor.requireUserId());
    }
}
