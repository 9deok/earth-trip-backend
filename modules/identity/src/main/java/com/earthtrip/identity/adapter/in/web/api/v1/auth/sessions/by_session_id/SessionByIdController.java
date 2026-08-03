package com.earthtrip.identity.adapter.in.web.api.v1.auth.sessions.by_session_id;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.SessionUseCase;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/sessions/{sessionId}")
class SessionByIdController {

    private final SessionUseCase useCase;
    private final CurrentUserProvider currentUser;

    SessionByIdController(SessionUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@PathVariable UUID sessionId) {
        useCase.revoke(sessionId, currentUser.requireUserId());
    }
}
