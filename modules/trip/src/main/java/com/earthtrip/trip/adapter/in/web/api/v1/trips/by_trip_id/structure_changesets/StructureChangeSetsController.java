package com.earthtrip.trip.adapter.in.web.api.v1.trips.by_trip_id.structure_changesets;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.TripStructureUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/structure-changesets")
class StructureChangeSetsController {

    private final TripStructureUseCase useCase;
    private final CurrentActor actor;

    StructureChangeSetsController(TripStructureUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TripStructureUseCase.ChangeSetResult post(
        @PathVariable UUID tripId,
        @Valid @RequestBody StructureChangeSetRequest request
    ) {
        return useCase.apply(
            tripId, actor.requireUserId(), request.toProposal(), request.expectedProposalHash()
        );
    }
}

record StructureChangeSetRequest(
    @NotNull UUID requestId,
    @PositiveOrZero long tripBaseVersion,
    LocalDate startDate,
    LocalDate endDate,
    @Valid List<StructureSegmentRequest> segments,
    @Valid List<StructureSegmentRemovalRequest> removedSegments,
    @NotBlank String expectedProposalHash
) {
    TripStructureUseCase.StructureProposal toProposal() {
        return new TripStructureUseCase.StructureProposal(
            requestId, tripBaseVersion, startDate, endDate,
            segments == null ? List.of() : segments.stream()
                .map(StructureSegmentRequest::toProposal)
                .toList(),
            removedSegments == null ? List.of() : removedSegments.stream()
                .map(StructureSegmentRemovalRequest::toRemoval)
                .toList()
        );
    }
}

record StructureSegmentRequest(
    @NotNull UUID segmentId,
    @NotBlank String type,
    String cityName,
    String countryCode,
    String placeId,
    BigDecimal latitude,
    BigDecimal longitude,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    String accommodationName,
    String accommodationPlaceId,
    Instant checkInAt,
    Instant checkOutAt,
    String transportMode,
    Instant departureAt,
    Instant arrivalAt,
    @PositiveOrZero int sortOrder,
    @PositiveOrZero long baseVersion
) {
    TripStructureUseCase.SegmentProposal toProposal() {
        return new TripStructureUseCase.SegmentProposal(
            segmentId, type, cityName, countryCode, placeId, latitude, longitude,
            startDate, endDate, accommodationName, accommodationPlaceId, checkInAt,
            checkOutAt, transportMode, departureAt, arrivalAt, sortOrder, baseVersion
        );
    }
}

record StructureSegmentRemovalRequest(
    @NotNull UUID segmentId,
    @PositiveOrZero long baseVersion
) {
    TripStructureUseCase.SegmentRemoval toRemoval() {
        return new TripStructureUseCase.SegmentRemoval(segmentId, baseVersion);
    }
}
