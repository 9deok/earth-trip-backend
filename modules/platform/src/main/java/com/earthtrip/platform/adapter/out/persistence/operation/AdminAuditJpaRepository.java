package com.earthtrip.platform.adapter.out.persistence.operation;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AdminAuditJpaRepository extends JpaRepository<AdminAuditJpaEntity, Long> {

    @Query(
            """
        select event from AdminAuditJpaEntity event
        where (:action is null or event.action = :action)
          and (:targetType is null or event.targetType = :targetType)
          and (:targetId is null or event.targetId = :targetId)
        order by event.occurredAt desc
        """)
    List<AdminAuditJpaEntity> search(
            @Param("action") String action,
            @Param("targetType") String targetType,
            @Param("targetId") String targetId,
            Pageable pageable);
}
