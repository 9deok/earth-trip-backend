package com.earthtrip.identity.adapter.in.web.api.v1.me.favorite_companions.by_favorite_id;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.FavoriteCompanionUseCase;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/favorite-companions/{favoriteId}")
class FavoriteCompanionByIdController {

    private final FavoriteCompanionUseCase useCase;
    private final CurrentUserProvider currentUser;

    FavoriteCompanionByIdController(
            FavoriteCompanionUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID favoriteId) {
        useCase.remove(currentUser.requireUserId(), favoriteId);
    }
}
