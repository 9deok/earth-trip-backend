package com.earthtrip.notification.adapter.out.persistence.notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface PushDeliveryAttemptJpaRepository
        extends JpaRepository<PushDeliveryAttemptJpaEntity, String> {

    Optional<PushDeliveryAttemptJpaEntity> findByNotificationIdAndDeviceId(
            String notificationId, String deviceId);

    List<PushDeliveryAttemptJpaEntity>
            findAllByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                    String status, Instant nextAttemptAt, Pageable pageable);
}
