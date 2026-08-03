package com.earthtrip.identity.adapter.in.web.api.v1.me.favorite_companions;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.FavoriteCompanionUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/favorite-companions")
class FavoriteCompanionsController {

    private final FavoriteCompanionUseCase useCase;
    private final CurrentUserProvider currentUser;

    FavoriteCompanionsController(
        FavoriteCompanionUseCase useCase,
        CurrentUserProvider currentUser
    ) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<FavoriteCompanionUseCase.FavoriteResult> get() {
        return useCase.list(currentUser.requireUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    FavoriteCompanionUseCase.FavoriteResult post(
        @Valid @RequestBody FavoriteCompanionRequest request
    ) {
        return useCase.add(
            currentUser.requireUserId(), request.requestId(), request.companionUserId(),
            request.displayName(), request.email()
        );
    }
}

record FavoriteCompanionRequest(
    @NotNull UUID requestId,
    UUID companionUserId,
    @Size(min = 1, max = 80) String displayName,
    @Email @Size(max = 320) String email
) { }
