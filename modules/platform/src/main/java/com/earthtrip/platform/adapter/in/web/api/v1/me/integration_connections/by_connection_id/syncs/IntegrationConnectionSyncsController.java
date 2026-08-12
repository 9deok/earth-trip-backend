package com.earthtrip.platform.adapter.in.web.api.v1.me.integration_connections.by_connection_id.syncs;

import com.earthtrip.platform.application.port.in.IntegrationUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/integration-connections/{connectionId}/syncs")
class IntegrationConnectionSyncsController {
    private final IntegrationUseCase u;
    private final CurrentActor a;

    IntegrationConnectionSyncsController(IntegrationUseCase u, CurrentActor a) {
        this.u = u;
        this.a = a;
    }

    @PostMapping
    IntegrationUseCase.SyncJobResult post(
            @PathVariable UUID connectionId, @Valid @RequestBody SyncRequest r) {
        return u.syncConnection(a.requireUserId(), connectionId, r.requestId(), r.payload());
    }
}

record SyncRequest(@NotNull UUID requestId, Map<String, Object> payload) {}
