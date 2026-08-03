package com.earthtrip.identity.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PreferenceStorePort {

    Optional<PreferenceRecord> find(UUID userId);

    PreferenceRecord save(PreferenceRecord preference);

    record PreferenceRecord(
        UUID userId,
        String locale,
        String defaultCurrency,
        String timeZone,
        boolean shareTicketNames,
        boolean sharePersonalExpense,
        boolean optionalAnalytics,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) { }
}
