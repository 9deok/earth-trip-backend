package com.earthtrip.identity.adapter.in.web.api.v1.me.email_change_confirmations;
import com.earthtrip.identity.application.port.in.AccountIdentityUseCase;import com.earthtrip.sharedkernel.security.CurrentActor;import jakarta.validation.Valid;import jakarta.validation.constraints.NotBlank;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/me/email-change-confirmations")
class EmailChangeConfirmationsController{private final AccountIdentityUseCase useCase;private final CurrentActor actor;EmailChangeConfirmationsController(AccountIdentityUseCase useCase,CurrentActor actor){this.useCase=useCase;this.actor=actor;}@PostMapping AccountIdentityUseCase.EmailChangeResult post(@Valid @RequestBody EmailChangeConfirmation request){return useCase.confirmEmailChange(actor.requireUserId(),request.token());}}
record EmailChangeConfirmation(@NotBlank String token){}
