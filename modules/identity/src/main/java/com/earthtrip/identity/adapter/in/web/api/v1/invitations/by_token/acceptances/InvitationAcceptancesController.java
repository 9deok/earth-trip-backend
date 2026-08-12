package com.earthtrip.identity.adapter.in.web.api.v1.invitations.by_token.acceptances;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.InvitationUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invitations/{token}/acceptances")
class InvitationAcceptancesController {
    private final InvitationUseCase useCase;
    private final CurrentUserProvider currentUser;

    InvitationAcceptancesController(InvitationUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void post(@PathVariable String token) {
        useCase.accept(token, currentUser.requireUserId());
    }
}
