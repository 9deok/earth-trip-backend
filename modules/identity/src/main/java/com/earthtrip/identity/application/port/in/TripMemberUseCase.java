package com.earthtrip.identity.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TripMemberUseCase {
    List<MemberResult> list(UUID tripId, UUID actorUserId);

    MemberResult changeRole(
            UUID tripId, UUID memberId, UUID actorUserId, String role, long baseVersion);

    void remove(UUID tripId, UUID memberId, UUID actorUserId, long baseVersion);

    void leave(UUID tripId, UUID actorUserId);

    void transferOwnership(UUID tripId, UUID actorUserId, UUID toMemberId, boolean confirmed);

    record MemberResult(
            UUID memberId,
            UUID userId,
            String displayName,
            String email,
            String role,
            String status,
            boolean currentUser,
            Instant joinedAt,
            long version) {}
}
