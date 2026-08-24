package com.earthtrip.platform.application.service.realtime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.earthtrip.trip.api.TripAccess;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripRealtimeSessionServiceTest {

    @Test
    void 실시간_세션_인가는_여행_조회_권한을_검증한다() {
        UUID tripId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        TripAccess tripAccess = mock(TripAccess.class);
        TripRealtimeSessionService service = new TripRealtimeSessionService(tripAccess);

        service.authorize(tripId, actorUserId);

        verify(tripAccess).requireViewer(tripId, actorUserId);
    }
}
