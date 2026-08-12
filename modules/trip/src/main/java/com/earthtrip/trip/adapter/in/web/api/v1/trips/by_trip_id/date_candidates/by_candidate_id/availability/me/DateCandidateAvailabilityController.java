package com.earthtrip.trip.adapter.in.web.api.v1.trips.by_trip_id.date_candidates.by_candidate_id.availability.me;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.DateCandidateUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/date-candidates/{candidateId}/availability/me")
class DateCandidateAvailabilityController {
    private final DateCandidateUseCase useCase;
    private final CurrentActor actor;

    DateCandidateAvailabilityController(DateCandidateUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @PutMapping
    AvailabilityResponse put(
            @PathVariable UUID tripId,
            @PathVariable UUID candidateId,
            @Valid @RequestBody AvailabilityRequest r) {
        DateCandidateUseCase.AvailabilityResult a =
                useCase.putAvailability(
                        tripId, candidateId, actor.requireUserId(), r.availability(), r.note());
        return new AvailabilityResponse(a.userId(), a.availability(), a.note(), a.updatedAt());
    }
}

record AvailabilityRequest(@NotBlank String availability, String note) {}

record AvailabilityResponse(UUID userId, String availability, String note, Instant updatedAt) {}
