package com.earthtrip.identity.adapter.in.web.api.v1.auth.password_reset_requests;

import com.earthtrip.identity.application.port.in.PasswordResetUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password-reset-requests")
class PasswordResetRequestsController {

    private final PasswordResetUseCase useCase;

    PasswordResetRequestsController(PasswordResetUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    PasswordResetRequestResponse post(@Valid @RequestBody PasswordResetRequest request) {
        PasswordResetUseCase.RequestResult result = useCase.request(request.email());
        return new PasswordResetRequestResponse(
                result.requestId(), result.expiresAt(), result.deliveryStatus());
    }
}

record PasswordResetRequest(@NotBlank @Email String email) {}

record PasswordResetRequestResponse(UUID requestId, Instant expiresAt, String deliveryStatus) {}
