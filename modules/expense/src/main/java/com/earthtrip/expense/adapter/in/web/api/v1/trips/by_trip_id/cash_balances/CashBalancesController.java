package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.cash_balances;

import com.earthtrip.expense.application.port.in.FinanceLedgerUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/cash-balances")
class CashBalancesController {
    private final FinanceLedgerUseCase useCase;
    private final CurrentActor actor;

    CashBalancesController(FinanceLedgerUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    List<FinanceLedgerUseCase.CashBalance> get(@PathVariable UUID tripId) {
        return useCase.balances(tripId, actor.requireUserId());
    }
}
