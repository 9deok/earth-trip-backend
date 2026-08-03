package com.earthtrip.wallet.adapter.in.web.api.v1.trips.by_trip_id.wallet.diagnostics;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.WalletDiagnosticUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/wallet/diagnostics")
class WalletDiagnosticsController {

    private final WalletDiagnosticUseCase useCase;
    private final CurrentActor actor;

    WalletDiagnosticsController(WalletDiagnosticUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<WalletDiagnosticUseCase.DiagnosticResult> get(@PathVariable UUID tripId) {
        return useCase.list(tripId, actor.requireUserId());
    }
}
