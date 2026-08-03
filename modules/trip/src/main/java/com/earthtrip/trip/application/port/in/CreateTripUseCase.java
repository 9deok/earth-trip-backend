package com.earthtrip.trip.application.port.in;

public interface CreateTripUseCase {

    CreateTripResult create(CreateTripCommand command);
}
