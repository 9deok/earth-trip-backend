package com.earthtrip.notification.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceStorePort {
    Optional<NotificationStoreRecords.PreferenceRecord> preference(UUID userId);

    NotificationStoreRecords.PreferenceRecord savePreference(
            NotificationStoreRecords.PreferenceRecord record);
}
