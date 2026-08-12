package com.earthtrip.platform.adapter.out.persistence.operation;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OperationalJobJpaRepository extends JpaRepository<OperationalJobJpaEntity, String> {

    @Query(
            """
        select job from OperationalJobJpaEntity job
        where (:status is null or job.status = :status)
          and (:jobType is null or job.jobType = :jobType)
        order by job.createdAt desc
        """)
    List<OperationalJobJpaEntity> search(
            @Param("status") String status, @Param("jobType") String jobType, Pageable pageable);
}
