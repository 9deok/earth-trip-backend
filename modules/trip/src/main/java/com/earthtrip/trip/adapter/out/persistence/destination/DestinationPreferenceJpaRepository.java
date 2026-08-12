package com.earthtrip.trip.adapter.out.persistence.destination;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface DestinationPreferenceJpaRepository
        extends JpaRepository<DestinationPreferenceJpaEntity, DestinationPreferenceId> {
    List<DestinationPreferenceJpaEntity> findAllByCandidateId(String candidateId);

    List<DestinationPreferenceJpaEntity> findAllByCandidateIdIn(Collection<String> candidateIds);
}
