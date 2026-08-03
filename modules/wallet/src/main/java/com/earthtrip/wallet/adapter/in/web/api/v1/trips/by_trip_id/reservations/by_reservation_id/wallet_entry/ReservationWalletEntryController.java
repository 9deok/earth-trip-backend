package com.earthtrip.wallet.adapter.in.web.api.v1.trips.by_trip_id.reservations.by_reservation_id.wallet_entry;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.ReservationWalletEntryUseCase;
import com.earthtrip.wallet.application.port.in.WalletRecordUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/reservations/{reservationId}/wallet-entry")
class ReservationWalletEntryController {

    private final ReservationWalletEntryUseCase useCase;
    private final CurrentActor actor;

    ReservationWalletEntryController(
        ReservationWalletEntryUseCase useCase,
        CurrentActor actor
    ) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PutMapping
    WalletRecordUseCase.RecordResult put(
        @PathVariable UUID tripId,
        @PathVariable UUID reservationId,
        @Valid @RequestBody ReservationWalletEntryRequest request
    ) {
        return useCase.put(
            tripId, reservationId, actor.requireUserId(),
            new ReservationWalletEntryUseCase.Command(
                request.requestId(), request.payload(), request.visibility(),
                request.sortOrder(), request.baseVersion()
            )
        );
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
        @PathVariable UUID tripId,
        @PathVariable UUID reservationId,
        @Valid @RequestBody ReservationWalletEntryDeleteRequest request
    ) {
        useCase.delete(
            tripId, reservationId, actor.requireUserId(), request.baseVersion()
        );
    }
}

record ReservationWalletEntryRequest(
    UUID requestId,
    @NotNull Map<String, Object> payload,
    String visibility,
    @PositiveOrZero Integer sortOrder,
    @PositiveOrZero long baseVersion
) { }

record ReservationWalletEntryDeleteRequest(@PositiveOrZero long baseVersion) { }
