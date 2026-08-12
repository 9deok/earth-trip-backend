package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.place_candidates.by_candidate_id.sources.by_source_id;

import com.earthtrip.planning.application.port.in.CandidateSourceLinkUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/place-candidates/{candidateId}/sources/{sourceId}")
class PlaceCandidateSourceByIdController {

    private final CandidateSourceLinkUseCase useCase;
    private final CurrentActor actor;

    PlaceCandidateSourceByIdController(CandidateSourceLinkUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PutMapping
    CandidateSourceLinkUseCase.LinkResult put(
            @PathVariable UUID tripId,
            @PathVariable UUID candidateId,
            @PathVariable UUID sourceId) {
        return useCase.link(tripId, candidateId, sourceId, actor.requireUserId());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @PathVariable UUID tripId,
            @PathVariable UUID candidateId,
            @PathVariable UUID sourceId) {
        useCase.unlink(tripId, candidateId, sourceId, actor.requireUserId());
    }
}
