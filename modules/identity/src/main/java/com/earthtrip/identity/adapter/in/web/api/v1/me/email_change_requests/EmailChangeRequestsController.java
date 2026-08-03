package com.earthtrip.identity.adapter.in.web.api.v1.me.email_change_requests;
import com.earthtrip.identity.application.port.in.AccountIdentityUseCase;import com.earthtrip.sharedkernel.security.CurrentActor;import jakarta.validation.Valid;import jakarta.validation.constraints.Email;import jakarta.validation.constraints.NotBlank;import jakarta.validation.constraints.Size;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/me/email-change-requests")
class EmailChangeRequestsController{private final AccountIdentityUseCase useCase;private final CurrentActor actor;EmailChangeRequestsController(AccountIdentityUseCase useCase,CurrentActor actor){this.useCase=useCase;this.actor=actor;}@PostMapping AccountIdentityUseCase.EmailChangeRequestResult post(@Valid @RequestBody EmailChangeRequest request){return useCase.requestEmailChange(actor.requireUserId(),request.newEmail());}}
record EmailChangeRequest(@NotBlank @Email @Size(max=320)String newEmail){}
