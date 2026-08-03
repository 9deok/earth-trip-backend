package com.earthtrip.planning.adapter.out.persistence.resource;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface PlanningActivityJpaRepository
    extends JpaRepository<PlanningActivityJpaEntity, Long> {

    List<PlanningActivityJpaEntity> findAllByTripIdAndSequenceIdGreaterThanOrderBySequenceIdAsc(
        String tripId,
        long sequenceId,
        Pageable pageable
    );

    Optional<PlanningActivityJpaEntity> findByEventId(String eventId);

    Optional<PlanningActivityJpaEntity> findFirstByTripIdOrderBySequenceIdDesc(String tripId);
}
