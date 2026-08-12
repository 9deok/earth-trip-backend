package com.earthtrip.identity.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TripMemberView {

    List<Member> members(UUID tripId, UUID actorUserId);

    record Member(
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
