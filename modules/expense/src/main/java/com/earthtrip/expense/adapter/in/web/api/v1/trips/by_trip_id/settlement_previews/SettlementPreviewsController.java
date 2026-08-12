package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.settlement_previews;

import com.earthtrip.expense.application.port.in.SettlementUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/settlement-previews")
class SettlementPreviewsController {
    private final SettlementUseCase useCase;
    private final CurrentActor actor;

    SettlementPreviewsController(SettlementUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @PostMapping
    SettlementUseCase.PreviewResult post(
            @PathVariable UUID tripId, @Valid @RequestBody PreviewMutation r) {
        return useCase.preview(tripId, actor.requireUserId(), r.baseCurrency(), r.minorUnitRates());
    }
}

record PreviewMutation(
        @NotBlank String baseCurrency,
        Map<String, @DecimalMin(value = "0", inclusive = false) BigDecimal> minorUnitRates) {}
