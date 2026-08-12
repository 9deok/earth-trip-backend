package com.earthtrip.planning.application.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.earthtrip.planning.application.port.in.ActivityFeedUseCase;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.planning.application.port.out.ActivityOperationStorePort;
import com.earthtrip.planning.application.port.out.SyncStateStorePort;
import com.earthtrip.trip.api.TripAccess;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActivityFeedServiceTest {

    @Test
    void changes는_활동_읽음_커서를_추가로_조회하지_않는다() {
        TripAccess access = mock(TripAccess.class);
        ActivityOperationStorePort activities = mock(ActivityOperationStorePort.class);
        SyncStateStorePort syncState = mock(SyncStateStorePort.class);
        UUID tripId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(activities.activities(tripId, 7, 3))
                .thenReturn(
                        List.of(
                                activity(8, tripId, actorId),
                                activity(9, tripId, actorId),
                                activity(10, tripId, actorId)));
        ActivityFeedService service = service(access, activities, syncState);

        ActivityFeedUseCase.ChangePage result = service.changes(tripId, actorId, 7L, 2);

        assertThat(result.changes()).hasSize(2);
        assertThat(result.nextCursor()).isEqualTo(9);
        assertThat(result.hasMore()).isTrue();
        verify(access).requireViewer(tripId, actorId);
        verify(activities).activities(tripId, 7, 3);
        verifyNoInteractions(syncState);
    }

    @Test
    void 최신_변경_커서는_전체_동기화_본문_없이_조회한다() {
        TripAccess access = mock(TripAccess.class);
        ActivityOperationStorePort activities = mock(ActivityOperationStorePort.class);
        SyncStateStorePort syncState = mock(SyncStateStorePort.class);
        UUID tripId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(activities.latestSequence(tripId)).thenReturn(42L);
        ActivityFeedService service = service(access, activities, syncState);

        ActivityFeedUseCase.ChangeCursorResult result = service.latestCursor(tripId, actorId);

        assertThat(result.cursor()).isEqualTo(42);
        verify(access).requireViewer(tripId, actorId);
        verify(activities).latestSequence(tripId);
        verifyNoInteractions(syncState);
    }

    private static ActivityFeedService service(
            TripAccess access,
            ActivityOperationStorePort activities,
            SyncStateStorePort syncState) {
        return new ActivityFeedService(
                access,
                mock(PlanningResourceUseCase.class),
                activities,
                syncState,
                Clock.fixed(Instant.parse("2026-08-06T00:00:00Z"), ZoneOffset.UTC));
    }

    private static ActivityOperationStorePort.ActivityRecord activity(
            long sequenceId, UUID tripId, UUID actorId) {
        return new ActivityOperationStorePort.ActivityRecord(
                sequenceId,
                UUID.randomUUID(),
                tripId,
                actorId,
                "UPDATED",
                "SCHEDULE_ITEM",
                UUID.randomUUID(),
                Map.of("version", sequenceId),
                Instant.parse("2026-08-06T00:00:00Z"));
    }
}
