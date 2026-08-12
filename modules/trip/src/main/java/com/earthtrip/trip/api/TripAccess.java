package com.earthtrip.trip.api;

import java.util.UUID;

public interface TripAccess {
    AccessResult requireViewer(UUID tripId, UUID actorUserId);

    AccessResult requireEditor(UUID tripId, UUID actorUserId);

    AccessResult requireOwner(UUID tripId, UUID actorUserId);

    AccessResult requireOwnerIncludingDeletionPending(UUID tripId, UUID actorUserId);

    AccessResult transferOwnership(UUID tripId, UUID currentOwnerUserId, UUID newOwnerUserId);

    PublicTripResult publicInfo(UUID tripId);

    record AccessResult(UUID tripId, UUID ownerUserId, String role, long tripVersion) {}

    record PublicTripResult(
            UUID tripId,
            String title,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            String timeZone,
            String status) {}
}
