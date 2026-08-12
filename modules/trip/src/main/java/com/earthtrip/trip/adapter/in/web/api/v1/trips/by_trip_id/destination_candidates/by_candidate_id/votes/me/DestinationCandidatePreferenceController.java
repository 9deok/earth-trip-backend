package com.earthtrip.trip.adapter.in.web.api.v1.trips.by_trip_id.destination_candidates.by_candidate_id.votes.me;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.DestinationCandidateUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/destination-candidates/{candidateId}/votes/me")
class DestinationCandidatePreferenceController {
    private final DestinationCandidateUseCase useCase;
    private final CurrentActor actor;

    DestinationCandidatePreferenceController(
            DestinationCandidateUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PutMapping
    PreferenceResponse put(
            @PathVariable UUID tripId,
            @PathVariable UUID candidateId,
            @Valid @RequestBody PreferenceRequest request) {
        DestinationCandidateUseCase.PreferenceResult p =
                useCase.putPreference(
                        tripId, candidateId, actor.requireUserId(), request.preference());
        return new PreferenceResponse(p.userId(), p.preference(), p.updatedAt());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID tripId, @PathVariable UUID candidateId) {
        useCase.deletePreference(tripId, candidateId, actor.requireUserId());
    }
}

record PreferenceRequest(@NotBlank String preference) {}

record PreferenceResponse(UUID userId, String preference, Instant updatedAt) {}
