package com.earthtrip.identity.adapter.in.web.api.v1.trips.by_trip_id.invitations.by_invitation_id.deliveries;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.InvitationUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/invitations/{invitationId}/deliveries")
class InvitationDeliveriesController {
    private final InvitationUseCase useCase;
    private final CurrentUserProvider currentUser;

    InvitationDeliveriesController(InvitationUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    InvitationDeliveryResponse post(
            @PathVariable UUID tripId,
            @PathVariable UUID invitationId,
            @Valid @RequestBody InvitationDeliveryRequest request) {
        InvitationUseCase.CreatedInvitation created =
                useCase.redeliver(
                        tripId, invitationId, currentUser.requireUserId(), request.baseVersion());
        InvitationUseCase.InvitationResult i = created.invitation();
        return new InvitationDeliveryResponse(
                i.invitationId(),
                i.deliveryStatus(),
                i.lastDeliveredAt(),
                i.expiresAt(),
                i.version(),
                created.token(),
                created.invitationUrl());
    }
}

record InvitationDeliveryRequest(@Min(0) long baseVersion) {}

record InvitationDeliveryResponse(
        UUID invitationId,
        String deliveryStatus,
        Instant deliveredAt,
        Instant expiresAt,
        long version,
        String token,
        String invitationUrl) {}
