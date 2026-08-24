package com.earthtrip.notification.adapter.out.persistence.notification;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, String> {
    List<NotificationJpaEntity> findAllByUserIdAndHiddenAtIsNullOrderByCreatedAtDesc(String user);

    long countByUserIdAndHiddenAtIsNullAndReadAtIsNull(String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
        update NotificationJpaEntity notification
           set notification.readAt = :readAt,
               notification.version = notification.version + 1
         where notification.userId = :userId
           and notification.hiddenAt is null
           and notification.id in :notificationIds
        """)
    int markManyRead(
            @Param("userId") String userId,
            @Param("notificationIds") Collection<String> notificationIds,
            @Param("readAt") Instant readAt);
}
