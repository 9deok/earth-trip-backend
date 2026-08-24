package com.earthtrip.platform.application.service.realtime;

import com.earthtrip.platform.application.port.in.TripRealtimeSessionUseCase;
import com.earthtrip.trip.api.TripAccess;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class TripRealtimeSessionService implements TripRealtimeSessionUseCase {

    private final TripAccess tripAccess;

    TripRealtimeSessionService(TripAccess tripAccess) {
        this.tripAccess = tripAccess;
    }

    @Override
    public void authorize(UUID tripId, UUID actorUserId) {
        tripAccess.requireViewer(tripId, actorUserId);
    }
}
