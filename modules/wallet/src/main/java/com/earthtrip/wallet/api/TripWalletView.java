package com.earthtrip.wallet.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TripWalletView {

    WalletSnapshot snapshot(UUID tripId, UUID actorUserId);

    PreparationSummary preparationSummary(UUID tripId, UUID actorUserId);

    BootstrapSnapshot bootstrap(UUID tripId, UUID actorUserId);

    record BootstrapSnapshot(WalletSnapshot wallet, PreparationSummary preparation) {}

    record WalletSnapshot(
            List<Entry> reservations,
            List<Entry> tickets,
            int offlineReadyCount,
            int actionRequiredCount) {}

    record Entry(
            UUID id,
            String type,
            Map<String, Object> payload,
            String status,
            String visibility,
            long version) {}

    record PreparationSummary(
            int completedCount,
            int totalCount,
            int remainingCount,
            List<PreparationItem> upcomingTasks) {}

    record PreparationItem(UUID id, Map<String, Object> payload, String status, long version) {}
}
