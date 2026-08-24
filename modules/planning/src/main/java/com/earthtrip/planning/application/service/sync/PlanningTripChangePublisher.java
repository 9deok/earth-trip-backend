package com.earthtrip.planning.application.service.sync;

import com.earthtrip.planning.application.port.out.ActivityOperationStorePort;
import com.earthtrip.trip.spi.TripChangePublisher;
import com.earthtrip.trip.spi.TripRealtimeNotifier;
import java.time.Clock;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Transactional
class PlanningTripChangePublisher implements TripChangePublisher {

    private final ActivityOperationStorePort activities;
    private final Clock clock;
    private final List<TripRealtimeNotifier> realtimeNotifiers;

    PlanningTripChangePublisher(
            ActivityOperationStorePort activities,
            Clock clock,
            List<TripRealtimeNotifier> realtimeNotifiers) {
        this.activities = activities;
        this.clock = clock;
        this.realtimeNotifiers = List.copyOf(realtimeNotifiers);
    }

    @Override
    public void publish(
            UUID tripId,
            UUID actorUserId,
            String action,
            String resourceType,
            UUID resourceId,
            Map<String, Object> details) {
        String normalizedAction = normalize(action);
        String normalizedResourceType = normalize(resourceType);
        Map<String, Object> safeDetails =
                details == null
                        ? Map.of()
                        : Collections.unmodifiableMap(new LinkedHashMap<>(details));
        activities.appendActivity(
                tripId,
                actorUserId,
                normalizedAction,
                normalizedResourceType,
                resourceId,
                safeDetails,
                clock.instant());
        notifyAfterCommit(tripId, normalizedAction, normalizedResourceType, resourceId);
    }

    private void notifyAfterCommit(
            UUID tripId, String action, String resourceType, UUID resourceId) {
        Runnable notification =
                () ->
                        realtimeNotifiers.forEach(
                                notifier ->
                                        notifier.notifyChange(
                                                tripId, action, resourceType, resourceId));
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notification.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notification.run();
                    }
                });
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("변경 유형이 필요합니다.");
        }
        return value.strip().toUpperCase(Locale.ROOT);
    }
}
