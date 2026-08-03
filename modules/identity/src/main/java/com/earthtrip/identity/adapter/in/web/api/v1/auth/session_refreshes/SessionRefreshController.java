package com.earthtrip.identity.adapter.in.web.api.v1.auth.session_refreshes;

import com.earthtrip.identity.application.port.in.SessionUseCase;
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
@RequestMapping("/api/v1/auth/session-refreshes")
class SessionRefreshController {

    private final SessionUseCase useCase;

    SessionRefreshController(SessionUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    SessionRefreshResponse refresh(@Valid @RequestBody SessionRefreshRequest request) {
        SessionUseCase.SessionResult result = useCase.refresh(request.refreshToken());
        return new SessionRefreshResponse(
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

record SessionRefreshRequest(@NotBlank @Size(max = 200) String refreshToken) { }

record SessionRefreshResponse(
    UUID sessionId,
    UUID userId,
    String tokenType,
    String accessToken,
    String refreshToken,
    Instant accessExpiresAt,
    Instant refreshExpiresAt
) { }
