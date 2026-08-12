package com.earthtrip.wallet.adapter.in.web.api.v1.trips.by_trip_id.reservations.by_reservation_id.change_previews;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.ReservationChangeUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/reservations/{reservationId}/change-previews")
class ReservationChangePreviewsController {

    private final ReservationChangeUseCase useCase;
    private final CurrentActor actor;

    ReservationChangePreviewsController(ReservationChangeUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    ReservationChangeUseCase.PreviewResult preview(
            @PathVariable UUID tripId,
            @PathVariable UUID reservationId,
            @Valid @RequestBody ReservationChangePreviewRequest request) {
        return useCase.preview(tripId, reservationId, actor.requireUserId(), request.command());
    }
}

record ReservationChangePreviewRequest(
        UUID requestId,
        Map<String, Object> reservationPayload,
        String visibility,
        @PositiveOrZero Integer sortOrder,
        @PositiveOrZero long reservationBaseVersion,
        Map<String, Object> walletEntryPayload,
        @PositiveOrZero long walletEntryBaseVersion) {
    ReservationChangeUseCase.ChangeCommand command() {
        return new ReservationChangeUseCase.ChangeCommand(
                requestId,
                reservationPayload,
                visibility,
                sortOrder,
                reservationBaseVersion,
                walletEntryPayload,
                walletEntryBaseVersion);
    }
}
