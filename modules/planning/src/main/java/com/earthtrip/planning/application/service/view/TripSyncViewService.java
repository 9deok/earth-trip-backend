package com.earthtrip.planning.application.service.view;

import com.earthtrip.planning.api.TripSyncView;
import com.earthtrip.planning.application.port.out.ActivityOperationStorePort;
import com.earthtrip.trip.api.TripAccess;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class TripSyncViewService implements TripSyncView {

    private final TripAccess access;
    private final ActivityOperationStorePort activities;

    TripSyncViewService(TripAccess access, ActivityOperationStorePort activities) {
        this.access = access;
        this.activities = activities;
    }

    @Override
    public long latestCursor(UUID tripId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        return activities.latestSequence(tripId);
    }
}
