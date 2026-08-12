package com.earthtrip.wallet.adapter.in.web.api.v1.trips.by_trip_id.wallet;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.WalletRecordUseCase;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/wallet")
class TripWalletController {
    private final WalletRecordUseCase useCase;
    private final CurrentActor actor;

    TripWalletController(WalletRecordUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    WalletRecordUseCase.WalletSummary get(@PathVariable UUID tripId) {
        return useCase.wallet(tripId, actor.requireUserId());
    }
}
