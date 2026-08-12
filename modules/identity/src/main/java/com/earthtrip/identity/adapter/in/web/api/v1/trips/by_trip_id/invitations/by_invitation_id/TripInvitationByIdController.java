package com.earthtrip.identity.adapter.in.web.api.v1.trips.by_trip_id.invitations.by_invitation_id;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.InvitationUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/invitations/{invitationId}")
class TripInvitationByIdController {
    private final InvitationUseCase useCase;
    private final CurrentUserProvider currentUser;

    TripInvitationByIdController(InvitationUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @GetMapping
    InvitationResponse get(@PathVariable UUID tripId, @PathVariable UUID invitationId) {
        return response(useCase.get(tripId, invitationId, currentUser.requireUserId()));
    }

    @PatchMapping
    InvitationResponse patch(
            @PathVariable UUID tripId,
            @PathVariable UUID invitationId,
            @Valid @RequestBody UpdateInvitationRequest request) {
        InvitationUseCase.InvitationResult i =
                useCase.update(
                        tripId,
                        invitationId,
                        currentUser.requireUserId(),
                        request.role(),
                        request.expiresAt(),
                        request.baseVersion());
        return response(i);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @PathVariable UUID tripId,
            @PathVariable UUID invitationId,
            @Valid @RequestBody RevokeInvitationRequest request) {
        useCase.revoke(tripId, invitationId, currentUser.requireUserId(), request.baseVersion());
    }

    private static InvitationResponse response(InvitationUseCase.InvitationResult i) {
        return new InvitationResponse(
                i.invitationId(),
                i.tripId(),
                i.email(),
                i.role(),
                i.status(),
                i.expiresAt(),
                i.deliveryStatus(),
                i.lastDeliveredAt(),
                i.createdAt(),
                i.version());
    }
}

record UpdateInvitationRequest(String role, Instant expiresAt, @Min(0) long baseVersion) {}

record RevokeInvitationRequest(@Min(0) long baseVersion) {}

record InvitationResponse(
        UUID invitationId,
        UUID tripId,
        String email,
        String role,
        String status,
        Instant expiresAt,
        String deliveryStatus,
        Instant lastDeliveredAt,
        Instant createdAt,
        long version) {}
