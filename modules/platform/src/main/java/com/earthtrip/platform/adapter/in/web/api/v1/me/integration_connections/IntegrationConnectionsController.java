package com.earthtrip.platform.adapter.in.web.api.v1.me.integration_connections;

import com.earthtrip.platform.application.port.in.IntegrationUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/integration-connections")
class IntegrationConnectionsController {
    private final IntegrationUseCase u;
    private final CurrentActor a;

    IntegrationConnectionsController(IntegrationUseCase u, CurrentActor a) {
        this.u = u;
        this.a = a;
    }

    @GetMapping
    List<IntegrationUseCase.ConnectionResult> get() {
        return u.connections(a.requireUserId(), "GENERAL");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    IntegrationUseCase.ConnectionResult post(@Valid @RequestBody ConnectionRequest r) {
        return u.createConnection(a.requireUserId(), "GENERAL", r.command());
    }
}

record ConnectionRequest(
        @NotNull UUID requestId,
        @NotBlank String provider,
        Set<String> scopes,
        Map<String, Object> metadata,
        String authorizationCode,
        String redirectUri,
        String codeVerifier) {
    IntegrationUseCase.ConnectionCommand command() {
        return new IntegrationUseCase.ConnectionCommand(
                requestId,
                provider,
                scopes,
                metadata,
                authorizationCode,
                redirectUri,
                codeVerifier);
    }
}
