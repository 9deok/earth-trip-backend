package com.earthtrip.platform.adapter.out.persistence.operation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface DeadLetterJpaRepository extends JpaRepository<DeadLetterJpaEntity, String> {

    Optional<DeadLetterJpaEntity> findFirstByJobIdAndStatusOrderByCreatedAtDesc(
            String jobId, String status);
}
