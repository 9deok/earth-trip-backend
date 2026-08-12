package com.earthtrip.identity.adapter.in.web.api.v1.trips.by_trip_id.invitations;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.InvitationUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/invitations")
class TripInvitationsController {
    private final InvitationUseCase useCase;
    private final CurrentUserProvider currentUser;

    TripInvitationsController(InvitationUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<InvitationResponse> get(@PathVariable UUID tripId) {
        return useCase.list(tripId, currentUser.requireUserId()).stream()
                .map(TripInvitationsController::response)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CreatedInvitationResponse post(
            @PathVariable UUID tripId, @Valid @RequestBody CreateInvitationRequest request) {
        InvitationUseCase.CreatedInvitation created =
                useCase.create(
                        tripId,
                        currentUser.requireUserId(),
                        request.requestId(),
                        request.email(),
                        request.role());
        return new CreatedInvitationResponse(
                response(created.invitation()), created.token(), created.invitationUrl());
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

record CreateInvitationRequest(
        @NotNull UUID requestId, @NotBlank @Email String email, String role) {}

record CreatedInvitationResponse(
        InvitationResponse invitation, String token, String invitationUrl) {}

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
