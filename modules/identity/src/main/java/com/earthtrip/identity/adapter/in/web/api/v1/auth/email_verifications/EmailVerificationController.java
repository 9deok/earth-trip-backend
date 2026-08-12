package com.earthtrip.identity.adapter.in.web.api.v1.auth.email_verifications;

import com.earthtrip.identity.application.port.in.EmailVerificationUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/email-verifications")
class EmailVerificationController {

    private final EmailVerificationUseCase useCase;

    EmailVerificationController(EmailVerificationUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    EmailVerificationResponse request(@Valid @RequestBody EmailVerificationRequest request) {
        EmailVerificationUseCase.RequestResult result = useCase.request(request.email());
        return new EmailVerificationResponse(
                result.requestId(), result.expiresAt(), result.deliveryStatus());
    }
}

record EmailVerificationRequest(@NotBlank @Email @Size(max = 320) String email) {}

record EmailVerificationResponse(UUID requestId, Instant expiresAt, String deliveryStatus) {}
