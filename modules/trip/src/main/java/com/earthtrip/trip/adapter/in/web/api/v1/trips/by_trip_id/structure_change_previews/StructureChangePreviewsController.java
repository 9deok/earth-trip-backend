package com.earthtrip.trip.adapter.in.web.api.v1.trips.by_trip_id.structure_change_previews;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/structure-change-previews")
class StructureChangePreviewsController {

    private final TripStructureUseCase useCase;
    private final CurrentActor actor;

    StructureChangePreviewsController(TripStructureUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    TripStructureUseCase.PreviewResult post(
            @PathVariable UUID tripId, @Valid @RequestBody StructureChangePreviewRequest request) {
        return useCase.preview(tripId, actor.requireUserId(), request.toProposal());
    }
}

record StructureChangePreviewRequest(
        @NotNull UUID requestId,
        @PositiveOrZero long tripBaseVersion,
        LocalDate startDate,
        LocalDate endDate,
        @Valid List<StructureSegmentRequest> segments,
        @Valid List<StructureSegmentRemovalRequest> removedSegments) {
    TripStructureUseCase.StructureProposal toProposal() {
        return new TripStructureUseCase.StructureProposal(
                requestId,
                tripBaseVersion,
                startDate,
                endDate,
                segments == null
                        ? List.of()
                        : segments.stream().map(StructureSegmentRequest::toProposal).toList(),
                removedSegments == null
                        ? List.of()
                        : removedSegments.stream()
                                .map(StructureSegmentRemovalRequest::toRemoval)
                                .toList());
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
        String timeZone,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String accommodationName,
        String accommodationPlaceId,
        Instant checkInAt,
        Instant checkOutAt,
        String transportMode,
        Instant departureAt,
        Instant arrivalAt,
        Instant anchorAt,
        @PositiveOrZero int sortOrder,
        @PositiveOrZero long baseVersion) {
    TripStructureUseCase.SegmentProposal toProposal() {
        return new TripStructureUseCase.SegmentProposal(
                segmentId,
                type,
                cityName,
                countryCode,
                placeId,
                latitude,
                longitude,
                timeZone,
                startDate,
                endDate,
                accommodationName,
                accommodationPlaceId,
                checkInAt,
                checkOutAt,
                transportMode,
                departureAt,
                arrivalAt,
                anchorAt,
                sortOrder,
                baseVersion);
    }
}

record StructureSegmentRemovalRequest(@NotNull UUID segmentId, @PositiveOrZero long baseVersion) {
    TripStructureUseCase.SegmentRemoval toRemoval() {
        return new TripStructureUseCase.SegmentRemoval(segmentId, baseVersion);
    }
}
