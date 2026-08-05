package com.earthtrip.planning.application.service.sync;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.earthtrip.planning.application.port.out.ActivityOperationStorePort;
import com.earthtrip.trip.api.TripRealtimeNotifier;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlanningTripChangePublisherTest {

    @Test
    void storesChangeAndNotifiesRealtimeSubscribers() {
        ActivityOperationStorePort activities = mock(ActivityOperationStorePort.class);
        TripRealtimeNotifier realtime = mock(TripRealtimeNotifier.class);
        UUID tripId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        PlanningTripChangePublisher publisher = new PlanningTripChangePublisher(
            activities,
            Clock.fixed(Instant.parse("2026-08-04T00:00:00Z"), ZoneOffset.UTC),
            List.of(realtime)
        );

        publisher.publish(
            tripId,
            actorId,
            "updated",
            "schedule_item",
            resourceId,
            Map.of("version", 2)
        );

        verify(activities).appendActivity(
            tripId,
            actorId,
            "UPDATED",
            "SCHEDULE_ITEM",
            resourceId,
            Map.of("version", 2),
            Instant.parse("2026-08-04T00:00:00Z")
        );
        verify(realtime).notifyChange(tripId, "UPDATED", "SCHEDULE_ITEM", resourceId);
    }
}
