package com.earthtrip.identity.adapter.in.web.api.v1.auth.registrations;

import com.earthtrip.identity.application.port.in.RegisterAccountUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/v1/auth/registrations")
class RegistrationController {

    private final RegisterAccountUseCase useCase;

    RegistrationController(RegisterAccountUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RegistrationResponse register(@Valid @RequestBody RegistrationRequest request) {
        RegisterAccountUseCase.Result result = useCase.register(new RegisterAccountUseCase.Command(
            request.requestId(),
            request.email(),
            request.password(),
            request.displayName()
        ));
        return new RegistrationResponse(
            result.userId(),
            result.email(),
            result.displayName(),
            result.status(),
            result.createdAt()
        );
    }
}

record RegistrationRequest(
    @NotNull UUID requestId,
    @NotBlank @Email @Size(max = 320) String email,
    @NotBlank @Size(min = 10, max = 128) String password,
    @NotBlank @Size(max = 80) String displayName
) { }

record RegistrationResponse(
    UUID userId,
    String email,
    String displayName,
    String status,
    Instant createdAt
) { }
