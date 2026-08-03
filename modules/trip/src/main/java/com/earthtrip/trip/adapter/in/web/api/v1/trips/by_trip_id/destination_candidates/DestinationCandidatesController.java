package com.earthtrip.trip.adapter.in.web.api.v1.trips.by_trip_id.destination_candidates;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.DestinationCandidateUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/trips/{tripId}/destination-candidates")
class DestinationCandidatesController {
    private final DestinationCandidateUseCase useCase; private final CurrentActor actor;
    DestinationCandidatesController(DestinationCandidateUseCase useCase, CurrentActor actor) {
        this.useCase = useCase; this.actor = actor;
    }
    @GetMapping List<DestinationCandidateResponse> get(@PathVariable UUID tripId) {
        return useCase.list(tripId, actor.requireUserId()).stream()
            .map(DestinationCandidatesController::response).toList();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    DestinationCandidateResponse post(
        @PathVariable UUID tripId, @Valid @RequestBody DestinationCandidateRequest r
    ) {
        return response(useCase.create(
            tripId, actor.requireUserId(), new DestinationCandidateUseCase.CandidateCommand(
                r.requestId(), r.name(), r.countryCode(), r.placeId(), r.latitude(), r.longitude(),
                r.note(), null, 0
            )
        ));
    }
    private static DestinationCandidateResponse response(DestinationCandidateUseCase.CandidateResult c) {
        return new DestinationCandidateResponse(
            c.candidateId(), c.tripId(), c.name(), c.countryCode(), c.placeId(), c.latitude(),
            c.longitude(), c.note(), c.status(), c.preferences().stream().map(p ->
                new PreferenceResponse(p.userId(), p.preference(), p.updatedAt())).toList(),
            c.version(), c.createdBy(), c.createdAt(), c.updatedAt()
        );
    }
}
record DestinationCandidateRequest(
    @NotNull UUID requestId, @NotBlank String name, String countryCode, String placeId,
    BigDecimal latitude, BigDecimal longitude, String note
) { }
record PreferenceResponse(UUID userId, String preference, Instant updatedAt) { }
record DestinationCandidateResponse(
    UUID candidateId, UUID tripId, String name, String countryCode, String placeId,
    BigDecimal latitude, BigDecimal longitude, String note, String status,
    List<PreferenceResponse> preferences, long version, UUID createdBy, Instant createdAt, Instant updatedAt
) { }
