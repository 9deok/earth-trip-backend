package com.earthtrip.trip.application.service.view;

import com.earthtrip.trip.api.TripStructureView;
import com.earthtrip.trip.application.port.in.TripManagementUseCase;
import com.earthtrip.trip.application.port.in.TripSegmentUseCase;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class TripStructureViewService implements TripStructureView {

    private final TripManagementUseCase trips;
    private final TripSegmentUseCase segments;

    TripStructureViewService(TripManagementUseCase trips, TripSegmentUseCase segments) {
        this.trips = trips;
        this.segments = segments;
    }

    @Override
    public StructureSnapshot snapshot(UUID tripId, UUID actorUserId) {
        TripManagementUseCase.TripResult trip = trips.get(tripId, actorUserId);
        return new StructureSnapshot(
                new Trip(
                        trip.tripId(),
                        trip.ownerUserId(),
                        trip.title(),
                        trip.status(),
                        trip.startDate(),
                        trip.endDate(),
                        trip.timeZone(),
                        trip.defaultCurrency(),
                        trip.planningMode(),
                        trip.pace(),
                        trip.version(),
                        trip.updatedAt()),
                segments.list(tripId, actorUserId).stream()
                        .map(
                                segment ->
                                        new Segment(
                                                segment.segmentId(),
                                                segment.type(),
                                                segment.cityName(),
                                                segment.countryCode(),
                                                segment.placeId(),
                                                segment.latitude(),
                                                segment.longitude(),
                                                segment.startDate(),
                                                segment.endDate(),
                                                segment.accommodationName(),
                                                segment.accommodationPlaceId(),
                                                segment.checkInAt(),
                                                segment.checkOutAt(),
                                                segment.transportMode(),
                                                segment.departureAt(),
                                                segment.arrivalAt(),
                                                segment.sortOrder(),
                                                segment.version()))
                        .toList());
    }
}
