package com.earthtrip.identity.adapter.in.web.api.v1.me.linked_identities.by_identity_id;
import com.earthtrip.identity.application.port.in.AccountIdentityUseCase;import com.earthtrip.sharedkernel.security.CurrentActor;import java.util.UUID;import org.springframework.http.HttpStatus;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/me/linked-identities/{identityId}")
class LinkedIdentityByIdController{private final AccountIdentityUseCase useCase;private final CurrentActor actor;LinkedIdentityByIdController(AccountIdentityUseCase useCase,CurrentActor actor){this.useCase=useCase;this.actor=actor;}@DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT)void delete(@PathVariable UUID identityId){useCase.unlink(actor.requireUserId(),identityId);}}
