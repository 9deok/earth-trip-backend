package com.earthtrip.trip.adapter.out.persistence.trip;

import java.util.Optional;

interface TripQuerydslSupport {

    Optional<TripJpaEntity> findById(String tripId);
}
