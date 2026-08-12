package com.earthtrip.platform.adapter.in.web.api.v1.trips.by_trip_id.provider_statement_imports;

import com.earthtrip.platform.application.port.in.IntegrationUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/provider-statement-imports")
class ProviderStatementImportsController {
    private final IntegrationUseCase u;
    private final CurrentActor a;

    ProviderStatementImportsController(IntegrationUseCase u, CurrentActor a) {
        this.u = u;
        this.a = a;
    }

    @PostMapping
    IntegrationUseCase.SyncJobResult post(
            @PathVariable UUID tripId, @Valid @RequestBody ProviderStatementRequest r) {
        return u.providerStatementImport(
                tripId, a.requireUserId(), r.requestId(), r.connectionId(), r.payload());
    }
}

record ProviderStatementRequest(
        @NotNull UUID requestId, @NotNull UUID connectionId, Map<String, Object> payload) {}
