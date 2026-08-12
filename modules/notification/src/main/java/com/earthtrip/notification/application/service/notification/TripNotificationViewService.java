package com.earthtrip.notification.application.service.notification;

import com.earthtrip.notification.api.TripNotificationView;
import com.earthtrip.notification.application.port.in.NotificationUseCase;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class TripNotificationViewService implements TripNotificationView {

    private final NotificationUseCase notifications;

    TripNotificationViewService(NotificationUseCase notifications) {
        this.notifications = notifications;
    }

    @Override
    public long unreadCount(UUID userId, UUID tripId) {
        return notifications.summary(userId).byTrip().getOrDefault(tripId, 0L);
    }
}
