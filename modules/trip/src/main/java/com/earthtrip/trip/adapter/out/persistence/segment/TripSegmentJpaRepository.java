package com.earthtrip.trip.adapter.out.persistence.segment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface TripSegmentJpaRepository extends JpaRepository<TripSegmentJpaEntity, String> {
    List<TripSegmentJpaEntity> findAllByTripIdOrderBySortOrderAsc(String tripId);
}
