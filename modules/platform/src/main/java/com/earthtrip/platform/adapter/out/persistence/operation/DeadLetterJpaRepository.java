package com.earthtrip.platform.adapter.out.persistence.operation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface DeadLetterJpaRepository extends JpaRepository<DeadLetterJpaEntity, String> {

    Optional<DeadLetterJpaEntity> findFirstByJobIdAndStatusOrderByCreatedAtDesc(
        String jobId,
        String status
    );

    @Query("""
        select event from DeadLetterJpaEntity event
        where (:status is null or event.status = :status)
        order by event.createdAt desc
        """)
    List<DeadLetterJpaEntity> search(
        @Param("status") String status,
        Pageable pageable
    );
}
