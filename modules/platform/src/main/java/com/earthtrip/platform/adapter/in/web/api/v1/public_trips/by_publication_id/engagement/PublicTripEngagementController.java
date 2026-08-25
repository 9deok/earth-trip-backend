package com.earthtrip.platform.adapter.in.web.api.v1.public_trips.by_publication_id.engagement;

import com.earthtrip.platform.application.port.in.PublicTripEngagementUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public-trips/{publicationId}/engagement")
class PublicTripEngagementController {
    private final PublicTripEngagementUseCase useCase;
    private final CurrentActor actor;

    PublicTripEngagementController(PublicTripEngagementUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    PublicTripEngagementUseCase.EngagementResult get(@PathVariable UUID publicationId) {
        return useCase.engagement(publicationId, actor.currentUserId().orElse(null));
    }
}
