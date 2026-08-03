package com.earthtrip.trip.application.port.in;

import java.time.Instant;
import java.util.UUID;

public record CreateTripResult(UUID tripId, String title, Instant createdAt) {
}
