package com.earthtrip.trip.application.port.out;

import com.earthtrip.trip.domain.Trip;
import com.earthtrip.trip.domain.TripId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadTripPort {

    Optional<Trip> findById(TripId tripId);

    List<Trip> findAllByOwner(UUID ownerUserId);

    List<Trip> findAllByIds(List<UUID> tripIds);
}
