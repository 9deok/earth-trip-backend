package com.earthtrip.platform.adapter.in.web.api.v1.public_trips.by_publication_id.copies;

import com.earthtrip.platform.application.port.in.PublicTripEngagementUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public-trips/{publicationId}/copies")
class PublicTripCopiesController {
    private final PublicTripEngagementUseCase useCase;
    private final CurrentActor actor;

    PublicTripCopiesController(PublicTripEngagementUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PublicTripEngagementUseCase.EngagementResult post(@PathVariable UUID publicationId) {
        return useCase.recordCopy(publicationId, actor.requireUserId());
    }
}
