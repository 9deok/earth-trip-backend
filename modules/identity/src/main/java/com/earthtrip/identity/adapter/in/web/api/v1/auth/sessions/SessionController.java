package com.earthtrip.identity.adapter.in.web.api.v1.auth.sessions;

import com.earthtrip.identity.application.port.in.SessionUseCase;
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
@RequestMapping("/api/v1/auth/sessions")
class SessionController {

    private final SessionUseCase useCase;

    SessionController(SessionUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SessionResponse create(@Valid @RequestBody SessionRequest request) {
        return SessionResponse.from(useCase.create(
            request.email(),
            request.password(),
            request.deviceName()
        ));
    }
}

record SessionRequest(
    @NotBlank @Email @Size(max = 320) String email,
    @NotBlank @Size(max = 128) String password,
    @Size(max = 120) String deviceName
) { }

record SessionResponse(
    UUID sessionId,
    UUID userId,
    String tokenType,
    String accessToken,
    String refreshToken,
    Instant accessExpiresAt,
    Instant refreshExpiresAt
) {
    static SessionResponse from(SessionUseCase.SessionResult result) {
        return new SessionResponse(
            result.sessionId(),
            result.userId(),
            "Bearer",
            result.accessToken(),
            result.refreshToken(),
            result.accessExpiresAt(),
            result.refreshExpiresAt()
        );
    }
}
