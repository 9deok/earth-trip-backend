package com.earthtrip.trip.adapter.out.persistence.destination;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface DestinationCandidateJpaRepository
        extends JpaRepository<DestinationCandidateJpaEntity, String> {
    List<DestinationCandidateJpaEntity> findAllByTripIdOrderByCreatedAtAsc(String tripId);
}
