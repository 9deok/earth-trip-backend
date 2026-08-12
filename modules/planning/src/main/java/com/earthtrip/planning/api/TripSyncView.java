package com.earthtrip.planning.api;

import java.util.UUID;

public interface TripSyncView {

    long latestCursor(UUID tripId, UUID actorUserId);
}
