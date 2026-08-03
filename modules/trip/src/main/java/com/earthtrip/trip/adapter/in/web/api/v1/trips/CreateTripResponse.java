package com.earthtrip.trip.adapter.in.web.api.v1.trips;

import com.earthtrip.trip.application.port.in.CreateTripResult;
import java.time.Instant;
import java.util.UUID;

record CreateTripResponse(UUID tripId, String title, Instant createdAt) {

    static CreateTripResponse from(CreateTripResult result) {
        return new CreateTripResponse(
            result.tripId(),
            result.title(),
            result.createdAt()
        );
    }
}
