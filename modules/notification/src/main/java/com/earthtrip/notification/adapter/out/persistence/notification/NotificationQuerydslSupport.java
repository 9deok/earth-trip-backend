package com.earthtrip.notification.adapter.out.persistence.notification;

import java.util.List;

interface NotificationQuerydslSupport {

    List<GroupCount> unreadByTrip(String userId);

    List<GroupCount> unreadByType(String userId);

    record GroupCount(String key, long total) {}
}
