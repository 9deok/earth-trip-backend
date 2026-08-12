package com.earthtrip.identity.adapter.in.web.api.v1.trips.by_trip_id.members;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.TripMemberUseCase;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/members")
class TripMembersController {
    private final TripMemberUseCase useCase;
    private final CurrentUserProvider currentUser;

    TripMembersController(TripMemberUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<MemberResponse> get(@PathVariable UUID tripId) {
        return useCase.list(tripId, currentUser.requireUserId()).stream()
                .map(TripMembersController::response)
                .toList();
    }

    private static MemberResponse response(TripMemberUseCase.MemberResult m) {
        return new MemberResponse(
                m.memberId(),
                m.userId(),
                m.displayName(),
                m.email(),
                m.role(),
                m.status(),
                m.currentUser(),
                m.joinedAt(),
                m.version());
    }
}

record MemberResponse(
        UUID memberId,
        UUID userId,
        String displayName,
        String email,
        String role,
        String status,
        boolean currentUser,
        Instant joinedAt,
        long version) {}
