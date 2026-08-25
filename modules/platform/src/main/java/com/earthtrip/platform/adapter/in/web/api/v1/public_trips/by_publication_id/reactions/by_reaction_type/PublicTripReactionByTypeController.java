package com.earthtrip.platform.adapter.in.web.api.v1.public_trips.by_publication_id.reactions.by_reaction_type;

import com.earthtrip.platform.application.port.in.PublicTripEngagementUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public-trips/{publicationId}/reactions/{reactionType}")
class PublicTripReactionByTypeController {
    private final PublicTripEngagementUseCase useCase;
    private final CurrentActor actor;

    PublicTripReactionByTypeController(PublicTripEngagementUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PutMapping
    PublicTripEngagementUseCase.EngagementResult put(
            @PathVariable UUID publicationId,
            @PathVariable @Pattern(regexp = "LIKE|HELPFUL") String reactionType,
            @Valid @RequestBody PublicTripReactionRequest request) {
        return useCase.setReaction(
                publicationId, actor.requireUserId(), reactionType, request.active());
    }
}

record PublicTripReactionRequest(@NotNull Boolean active) {}
