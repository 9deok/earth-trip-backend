package com.earthtrip.trip.adapter.in.web.api.v1.trips.by_trip_id.segment_order;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.TripSegmentUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/segment-order")
class SegmentOrderController {
    private final TripSegmentUseCase useCase;
    private final CurrentActor currentActor;

    SegmentOrderController(TripSegmentUseCase useCase, CurrentActor currentActor) {
        this.useCase = useCase; this.currentActor = currentActor;
    }

    @PutMapping
    List<SegmentOrderResponse> put(
        @PathVariable UUID tripId,
        @Valid @RequestBody SegmentOrderRequest request
    ) {
        return useCase.reorder(
            tripId,
            currentActor.requireUserId(),
            request.items().stream().map(item ->
                new TripSegmentUseCase.OrderItem(item.segmentId(), item.sortOrder(), item.baseVersion())
            ).toList()
        ).stream().map(segment -> new SegmentOrderResponse(
            segment.segmentId(), segment.sortOrder(), segment.version()
        )).toList();
    }
}

record SegmentOrderRequest(@NotEmpty List<@Valid SegmentOrderItemRequest> items) { }
record SegmentOrderItemRequest(@NotNull UUID segmentId, @Min(0) int sortOrder, @Min(0) long baseVersion) { }
record SegmentOrderResponse(UUID segmentId, int sortOrder, long version) { }
