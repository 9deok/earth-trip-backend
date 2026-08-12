package com.earthtrip.identity.adapter.in.web.api.v1.me.sessions;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.SessionUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/sessions")
class MeSessionsController {

    private final SessionUseCase useCase;
    private final CurrentUserProvider currentUser;

    MeSessionsController(SessionUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@RequestParam(defaultValue = "false") boolean includeCurrent) {
        useCase.revokeOtherSessions(
                currentUser.requireUserId(), currentUser.requireSessionId(), includeCurrent);
    }
}
