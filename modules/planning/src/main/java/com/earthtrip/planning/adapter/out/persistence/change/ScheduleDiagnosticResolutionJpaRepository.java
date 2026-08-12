package com.earthtrip.planning.adapter.out.persistence.change;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ScheduleDiagnosticResolutionJpaRepository
        extends JpaRepository<ScheduleDiagnosticResolutionJpaEntity, String> {

    List<ScheduleDiagnosticResolutionJpaEntity> findAllByTripIdAndDayId(
            String tripId, String dayId);
}
