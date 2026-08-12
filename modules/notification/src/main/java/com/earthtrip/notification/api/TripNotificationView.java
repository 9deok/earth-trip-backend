package com.earthtrip.notification.api;

import java.util.UUID;

public interface TripNotificationView {

    long unreadCount(UUID userId, UUID tripId);
}
