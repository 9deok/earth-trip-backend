package com.earthtrip.platform.adapter.in.web.api.v1.shared_trips.by_token.password_verifications;

import com.earthtrip.platform.application.port.in.SharedTripAccessUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shared-trips/{token}/password-verifications")
class SharedTripPasswordVerificationsController {

    private final SharedTripAccessUseCase useCase;

    SharedTripPasswordVerificationsController(SharedTripAccessUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SharedTripAccessUseCase.PasswordSessionResult post(
            @PathVariable String token, @Valid @RequestBody SharePasswordRequest request) {
        return useCase.verifyPassword(token, request.password());
    }
}

record SharePasswordRequest(@NotBlank @Size(max = 128) String password) {}
