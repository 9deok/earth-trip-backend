package com.earthtrip.identity.adapter.in.web.api.v1.auth.email_verification_confirmations;

import com.earthtrip.identity.application.port.in.EmailVerificationUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/email-verification-confirmations")
class EmailVerificationConfirmationController {

    private final EmailVerificationUseCase useCase;

    EmailVerificationConfirmationController(EmailVerificationUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    EmailVerificationConfirmationResponse confirm(
            @Valid @RequestBody EmailVerificationConfirmationRequest request) {
        EmailVerificationUseCase.ConfirmResult result = useCase.confirm(request.token());
        return new EmailVerificationConfirmationResponse(
                result.userId(), result.email(), result.verifiedAt());
    }
}

record EmailVerificationConfirmationRequest(@NotBlank @Size(max = 200) String token) {}

record EmailVerificationConfirmationResponse(UUID userId, String email, Instant verifiedAt) {}
