package com.earthtrip.identity.adapter.in.web.api.v1.trips.by_trip_id.members.by_member_id;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.TripMemberUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/trips/{tripId}/members/{memberId}")
class TripMemberByIdController {
    private final TripMemberUseCase useCase; private final CurrentUserProvider currentUser;
    TripMemberByIdController(TripMemberUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase; this.currentUser = currentUser;
    }
    @PatchMapping MemberResponse patch(
        @PathVariable UUID tripId, @PathVariable UUID memberId,
        @Valid @RequestBody MemberRoleRequest request
    ) {
        TripMemberUseCase.MemberResult m = useCase.changeRole(
            tripId, memberId, currentUser.requireUserId(), request.role(), request.baseVersion()
        );
        return new MemberResponse(
            m.memberId(), m.userId(), m.displayName(), m.email(), m.role(), m.status(),
            m.currentUser(), m.joinedAt(), m.version()
        );
    }
    @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
        @PathVariable UUID tripId, @PathVariable UUID memberId,
        @Valid @RequestBody MemberDeleteRequest request
    ) {
        useCase.remove(tripId, memberId, currentUser.requireUserId(), request.baseVersion());
    }
}
record MemberRoleRequest(@NotBlank String role, @Min(0) long baseVersion) { }
record MemberDeleteRequest(@Min(0) long baseVersion) { }
record MemberResponse(
    UUID memberId, UUID userId, String displayName, String email, String role, String status,
    boolean currentUser, Instant joinedAt, long version
) { }
