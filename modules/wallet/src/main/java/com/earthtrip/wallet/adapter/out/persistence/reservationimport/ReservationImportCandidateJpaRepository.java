package com.earthtrip.wallet.adapter.out.persistence.reservationimport;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReservationImportCandidateJpaRepository
    extends JpaRepository<ReservationImportCandidateJpaEntity, String> {

    List<ReservationImportCandidateJpaEntity> findAllByJobIdOrderByCreatedAtAsc(String jobId);
}
