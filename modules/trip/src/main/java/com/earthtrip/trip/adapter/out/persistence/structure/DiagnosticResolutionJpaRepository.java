package com.earthtrip.trip.adapter.out.persistence.structure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface DiagnosticResolutionJpaRepository
    extends JpaRepository<DiagnosticResolutionJpaEntity, String> {

    List<DiagnosticResolutionJpaEntity> findAllByTripId(String tripId);
}
