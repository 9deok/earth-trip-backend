package com.earthtrip.wallet.adapter.in.web.api.v1.trips.by_trip_id.reservations.by_reservation_id;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.WalletRecordUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/reservations/{reservationId}")
class ReservationByIdController {
    private final WalletRecordUseCase useCase;
    private final CurrentActor actor;

    ReservationByIdController(WalletRecordUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    WalletRecordUseCase.RecordResult get(
            @PathVariable UUID tripId, @PathVariable UUID reservationId) {
        return useCase.get(tripId, actor.requireUserId(), "RESERVATION", reservationId);
    }

    @PatchMapping
    WalletRecordUseCase.RecordResult patch(
            @PathVariable UUID tripId,
            @PathVariable UUID reservationId,
            @Valid @RequestBody ReservationMutation r) {
        return useCase.update(
                tripId,
                actor.requireUserId(),
                "RESERVATION",
                reservationId,
                false,
                new WalletRecordUseCase.Command(
                        reservationId,
                        null,
                        r.payload(),
                        r.status(),
                        r.visibility(),
                        r.sortOrder(),
                        r.baseVersion()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @PathVariable UUID tripId,
            @PathVariable UUID reservationId,
            @Valid @RequestBody ReservationDelete r) {
        useCase.delete(
                tripId,
                actor.requireUserId(),
                "RESERVATION",
                reservationId,
                false,
                r.baseVersion());
    }
}

record ReservationMutation(
        Map<String, Object> payload,
        String status,
        String visibility,
        Integer sortOrder,
        @Min(0) long baseVersion) {}

record ReservationDelete(@Min(0) long baseVersion) {}
