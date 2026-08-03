package com.earthtrip.trip.adapter.in.web.api.v1.trips.by_trip_id.destination_candidates.by_candidate_id;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.DestinationCandidateUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/trips/{tripId}/destination-candidates/{candidateId}")
class DestinationCandidateByIdController {
    private final DestinationCandidateUseCase useCase; private final CurrentActor actor;
    DestinationCandidateByIdController(DestinationCandidateUseCase useCase, CurrentActor actor) {
        this.useCase = useCase; this.actor = actor;
    }
    @PatchMapping DestinationCandidateResponse patch(
        @PathVariable UUID tripId, @PathVariable UUID candidateId,
        @Valid @RequestBody DestinationCandidateUpdateRequest r
    ) {
        return response(useCase.update(
            tripId, candidateId, actor.requireUserId(),
            new DestinationCandidateUseCase.CandidateCommand(
                candidateId, r.name(), r.countryCode(), r.placeId(), r.latitude(), r.longitude(),
                r.note(), r.status(), r.baseVersion()
            )
        ));
    }
    @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
        @PathVariable UUID tripId, @PathVariable UUID candidateId,
        @Valid @RequestBody CandidateDeleteRequest r
    ) { useCase.delete(tripId, candidateId, actor.requireUserId(), r.baseVersion()); }
    private static DestinationCandidateResponse response(DestinationCandidateUseCase.CandidateResult c) {
        return new DestinationCandidateResponse(
            c.candidateId(), c.tripId(), c.name(), c.countryCode(), c.placeId(), c.latitude(),
            c.longitude(), c.note(), c.status(), c.preferences().stream().map(p ->
                new PreferenceResponse(p.userId(), p.preference(), p.updatedAt())).toList(),
            c.version(), c.createdBy(), c.createdAt(), c.updatedAt()
        );
    }
}
record DestinationCandidateUpdateRequest(
    String name, String countryCode, String placeId, BigDecimal latitude,
    BigDecimal longitude, String note, String status, @Min(0) long baseVersion
) { }
record CandidateDeleteRequest(@Min(0) long baseVersion) { }
record PreferenceResponse(UUID userId, String preference, Instant updatedAt) { }
record DestinationCandidateResponse(
    UUID candidateId, UUID tripId, String name, String countryCode, String placeId,
    BigDecimal latitude, BigDecimal longitude, String note, String status,
    List<PreferenceResponse> preferences, long version, UUID createdBy, Instant createdAt, Instant updatedAt
) { }
