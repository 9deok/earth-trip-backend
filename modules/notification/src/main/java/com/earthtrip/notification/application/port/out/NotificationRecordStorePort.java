package com.earthtrip.notification.application.port.out;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRecordStorePort {
    List<NotificationStoreRecords.NotificationRecord> list(UUID userId);

    Optional<NotificationStoreRecords.NotificationRecord> find(UUID notificationId);

    NotificationStoreRecords.NotificationRecord save(
            NotificationStoreRecords.NotificationRecord record);

    default int markManyRead(UUID userId, List<UUID> notificationIds, Instant readAt) {
        int updated = 0;
        for (UUID notificationId : notificationIds) {
            Optional<NotificationStoreRecords.NotificationRecord> current =
                    find(notificationId)
                            .filter(item -> item.userId().equals(userId))
                            .filter(item -> item.hiddenAt() == null);
            if (current.isEmpty()) {
                continue;
            }
            NotificationStoreRecords.NotificationRecord item = current.get();
            save(
                    new NotificationStoreRecords.NotificationRecord(
                            item.id(),
                            item.userId(),
                            item.tripId(),
                            item.type(),
                            item.title(),
                            item.body(),
                            item.deepLink(),
                            item.metadata(),
                            item.createdAt(),
                            readAt,
                            item.hiddenAt(),
                            item.version()));
            updated += 1;
        }
        return updated;
    }

    default NotificationStoreRecords.NotificationSummaryRecord summary(UUID userId) {
        Map<UUID, Long> byTrip = new LinkedHashMap<>();
        Map<String, Long> byType = new LinkedHashMap<>();
        long total = 0;
        for (NotificationStoreRecords.NotificationRecord item : list(userId)) {
            if (item.readAt() != null) {
                continue;
            }
            total += 1;
            if (item.tripId() != null) {
                byTrip.merge(item.tripId(), 1L, Long::sum);
            }
            byType.merge(item.type(), 1L, Long::sum);
        }
        return new NotificationStoreRecords.NotificationSummaryRecord(
                total, Map.copyOf(byTrip), Map.copyOf(byType));
    }
}
