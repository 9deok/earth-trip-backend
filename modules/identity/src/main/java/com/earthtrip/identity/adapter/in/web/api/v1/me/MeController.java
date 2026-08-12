package com.earthtrip.identity.adapter.in.web.api.v1.me;

import com.earthtrip.identity.application.port.in.CurrentAccountUseCase;
import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
class MeController {

    private final CurrentAccountUseCase useCase;
    private final CurrentUserProvider currentUser;

    MeController(CurrentAccountUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @GetMapping
    MeResponse get() {
        return MeResponse.from(useCase.get(currentUser.requireUserId()));
    }

    @PatchMapping
    MeResponse update(@Valid @RequestBody UpdateMeRequest request) {
        return MeResponse.from(
                useCase.updateName(currentUser.requireUserId(), request.displayName()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    AccountDeletionResponse requestDeletion() {
        CurrentAccountUseCase.DeletionResult result =
                useCase.requestDeletion(currentUser.requireUserId());
        return new AccountDeletionResponse(
                result.requestId(),
                result.requestedAt(),
                result.scheduledDeletionAt(),
                result.status());
    }
}

record UpdateMeRequest(@NotBlank @Size(max = 80) String displayName) {}

record MeResponse(
        UUID userId,
        String email,
        String displayName,
        String status,
        Instant emailVerifiedAt,
        Instant createdAt,
        Instant updatedAt) {
    static MeResponse from(CurrentAccountUseCase.AccountResult result) {
        return new MeResponse(
                result.userId(),
                result.email(),
                result.displayName(),
                result.status(),
                result.emailVerifiedAt(),
                result.createdAt(),
                result.updatedAt());
    }
}

record AccountDeletionResponse(
        UUID requestId, Instant requestedAt, Instant scheduledDeletionAt, String status) {}
