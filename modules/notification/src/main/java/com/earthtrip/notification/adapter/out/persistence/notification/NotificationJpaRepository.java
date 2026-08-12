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

    @Query(
            """
        select notification.tripId as key, count(notification) as total
          from NotificationJpaEntity notification
         where notification.userId = :userId
           and notification.hiddenAt is null
           and notification.readAt is null
           and notification.tripId is not null
         group by notification.tripId
        """)
    List<GroupCount> unreadByTrip(@Param("userId") String userId);

    @Query(
            """
        select notification.type as key, count(notification) as total
          from NotificationJpaEntity notification
         where notification.userId = :userId
           and notification.hiddenAt is null
           and notification.readAt is null
         group by notification.type
        """)
    List<GroupCount> unreadByType(@Param("userId") String userId);

    interface GroupCount {
        String getKey();

        long getTotal();
    }
}
