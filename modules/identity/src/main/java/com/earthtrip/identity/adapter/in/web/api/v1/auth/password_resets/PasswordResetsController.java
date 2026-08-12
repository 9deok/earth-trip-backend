package com.earthtrip.identity.adapter.in.web.api.v1.auth.password_resets;

import com.earthtrip.identity.application.port.in.PasswordResetUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password-resets")
class PasswordResetsController {

    private final PasswordResetUseCase useCase;

    PasswordResetsController(PasswordResetUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void post(@Valid @RequestBody PasswordResetRequest request) {
        useCase.reset(request.token(), request.newPassword());
    }
}

record PasswordResetRequest(
        @NotBlank String token, @NotBlank @Size(min = 10, max = 128) String newPassword) {}
