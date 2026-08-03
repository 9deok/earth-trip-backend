package com.earthtrip.trip.application.port.out;

import com.earthtrip.trip.domain.TripId;
import com.earthtrip.trip.domain.TripSegment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripSegmentStorePort {
    List<TripSegment> findAll(TripId tripId);
    Optional<TripSegment> findById(UUID segmentId);
    TripSegment save(TripSegment segment);
    void delete(UUID segmentId);
}
