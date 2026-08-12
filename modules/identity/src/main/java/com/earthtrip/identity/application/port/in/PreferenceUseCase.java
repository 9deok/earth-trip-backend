package com.earthtrip.identity.application.port.in;

import java.time.Instant;
import java.util.UUID;

public interface PreferenceUseCase {

    PreferenceResult get(UUID userId);

    PreferenceResult update(UUID userId, UpdatePreferenceCommand command);

    record UpdatePreferenceCommand(
            String locale,
            String defaultCurrency,
            String timeZone,
            Boolean shareTicketNames,
            Boolean sharePersonalExpense,
            Boolean optionalAnalytics) {}

    record PreferenceResult(
            String locale,
            String defaultCurrency,
            String timeZone,
            boolean shareTicketNames,
            boolean sharePersonalExpense,
            boolean optionalAnalytics,
            long version,
            Instant updatedAt) {}
}
