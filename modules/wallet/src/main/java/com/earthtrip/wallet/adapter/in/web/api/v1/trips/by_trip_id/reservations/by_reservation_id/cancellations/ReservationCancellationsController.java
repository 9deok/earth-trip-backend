package com.earthtrip.wallet.adapter.in.web.api.v1.trips.by_trip_id.reservations.by_reservation_id.cancellations;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.WalletRecordUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/reservations/{reservationId}/cancellations")
class ReservationCancellationsController {
    private final WalletRecordUseCase useCase;
    private final CurrentActor actor;

    ReservationCancellationsController(WalletRecordUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    WalletRecordUseCase.RecordResult post(
            @PathVariable UUID tripId,
            @PathVariable UUID reservationId,
            @Valid @RequestBody CancellationMutation r) {
        useCase.get(tripId, actor.requireUserId(), "RESERVATION", reservationId);
        return useCase.create(
                tripId,
                actor.requireUserId(),
                "CANCELLATION",
                false,
                new WalletRecordUseCase.Command(
                        r.requestId(), reservationId, r.payload(), "REQUESTED", "TRIP", 0, 0));
    }
}

record CancellationMutation(@NotNull UUID requestId, @NotNull Map<String, Object> payload) {}
