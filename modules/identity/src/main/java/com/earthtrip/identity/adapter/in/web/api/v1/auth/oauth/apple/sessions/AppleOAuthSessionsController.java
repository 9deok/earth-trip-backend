package com.earthtrip.identity.adapter.in.web.api.v1.auth.oauth.apple.sessions;

import com.earthtrip.identity.application.port.in.AccountIdentityUseCase;
import com.earthtrip.identity.application.port.in.SessionUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/oauth/apple/sessions")
class AppleOAuthSessionsController {
    private final AccountIdentityUseCase useCase;
    AppleOAuthSessionsController(AccountIdentityUseCase useCase) { this.useCase = useCase; }
    @PostMapping SessionUseCase.SessionResult post(@Valid @RequestBody AppleOAuthRequest request) {
        return useCase.oauthSession("APPLE", request.command());
    }
}
record AppleOAuthRequest(
    String authorizationCode, String idToken, String redirectUri, String codeVerifier,
    @Size(max = 120) String deviceName
) {
    AccountIdentityUseCase.OAuthCommand command() {
        return new AccountIdentityUseCase.OAuthCommand(
            authorizationCode, idToken, redirectUri, codeVerifier, deviceName
        );
    }
}
