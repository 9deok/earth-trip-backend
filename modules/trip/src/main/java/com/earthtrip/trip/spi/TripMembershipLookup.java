package com.earthtrip.trip.spi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripMembershipLookup {
    Optional<String> activeRole(UUID tripId, UUID userId);
    List<UUID> activeTripIds(UUID userId);
}
