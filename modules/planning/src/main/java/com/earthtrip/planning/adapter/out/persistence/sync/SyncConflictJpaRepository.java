package com.earthtrip.planning.adapter.out.persistence.sync;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SyncConflictJpaRepository extends JpaRepository<SyncConflictJpaEntity, String> {

    List<SyncConflictJpaEntity> findAllByTripIdAndStatusOrderByCreatedAtAsc(
        String tripId,
        String status
    );
}
