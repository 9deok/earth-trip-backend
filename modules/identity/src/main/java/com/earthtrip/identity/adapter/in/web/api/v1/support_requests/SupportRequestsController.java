package com.earthtrip.identity.adapter.in.web.api.v1.support_requests;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.SupportRequestUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/support-requests")
class SupportRequestsController {

    private final SupportRequestUseCase useCase;
    private final CurrentUserProvider currentUser;

    SupportRequestsController(
        SupportRequestUseCase useCase,
        CurrentUserProvider currentUser
    ) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SupportRequestUseCase.SupportResult post(
        @Valid @RequestBody SupportRequest request
    ) {
        return useCase.create(
            currentUser.requireUserId(), request.requestId(), request.category(),
            request.description(), request.traceId(), request.diagnostics(),
            request.diagnosticsConsent()
        );
    }
}

record SupportRequest(
    @NotNull UUID requestId,
    @NotBlank String category,
    @NotBlank @Size(max = 5000) String description,
    @Size(max = 100) String traceId,
    Map<String, Object> diagnostics,
    boolean diagnosticsConsent
) { }
