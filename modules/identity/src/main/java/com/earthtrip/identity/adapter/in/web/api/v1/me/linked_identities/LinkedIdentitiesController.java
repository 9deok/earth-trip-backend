package com.earthtrip.identity.adapter.in.web.api.v1.me.linked_identities;

import com.earthtrip.identity.application.port.in.AccountIdentityUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/linked-identities")
class LinkedIdentitiesController {
    private final AccountIdentityUseCase useCase; private final CurrentActor actor;
    LinkedIdentitiesController(AccountIdentityUseCase useCase,CurrentActor actor){
        this.useCase=useCase;this.actor=actor;
    }
    @GetMapping List<AccountIdentityUseCase.IdentityResult> get(){
        return useCase.list(actor.requireUserId());
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    AccountIdentityUseCase.IdentityResult post(@Valid @RequestBody LinkedIdentityRequest request){
        return useCase.link(actor.requireUserId(),request.provider(),request.command());
    }
}
record LinkedIdentityRequest(
    @NotBlank String provider,String authorizationCode,String idToken,String redirectUri,
    String codeVerifier,@Size(max=120)String deviceName
){
 AccountIdentityUseCase.OAuthCommand command(){return new AccountIdentityUseCase.OAuthCommand(
     authorizationCode,idToken,redirectUri,codeVerifier,deviceName);}
}
