package com.earthtrip.trip.adapter.out.persistence.datecandidate;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface DateAvailabilityJpaRepository
        extends JpaRepository<DateAvailabilityJpaEntity, DateAvailabilityId> {
    List<DateAvailabilityJpaEntity> findAllByCandidateId(String candidateId);

    List<DateAvailabilityJpaEntity> findAllByCandidateIdIn(Collection<String> candidateIds);
}
