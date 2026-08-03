package com.earthtrip.trip.application.port.out;

import com.earthtrip.trip.domain.Trip;

public interface SaveTripPort {

    Trip save(Trip trip);
}
