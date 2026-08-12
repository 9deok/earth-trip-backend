package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.settlements.by_settlement_id.reopenings;

import com.earthtrip.expense.application.port.in.SettlementUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/settlements/{settlementId}/reopenings")
class SettlementReopeningsController {
    private final SettlementUseCase useCase;
    private final CurrentActor actor;

    SettlementReopeningsController(SettlementUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SettlementUseCase.SettlementResult post(
            @PathVariable UUID tripId,
            @PathVariable UUID settlementId,
            @Valid @RequestBody ReopeningMutation r) {
        return useCase.reopen(
                tripId, settlementId, actor.requireUserId(), r.baseVersion(), r.reason());
    }
}

record ReopeningMutation(@Min(0) long baseVersion, @NotBlank String reason) {}
