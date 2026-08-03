package com.earthtrip.identity.adapter.in.web.api.v1.invitations.by_token.declines;

import com.earthtrip.identity.application.port.in.InvitationUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/invitations/{token}/declines")
class InvitationDeclinesController {
    private final InvitationUseCase useCase;
    InvitationDeclinesController(InvitationUseCase useCase) { this.useCase = useCase; }
    @PostMapping @ResponseStatus(HttpStatus.NO_CONTENT)
    void post(@PathVariable String token) { useCase.decline(token); }
}
