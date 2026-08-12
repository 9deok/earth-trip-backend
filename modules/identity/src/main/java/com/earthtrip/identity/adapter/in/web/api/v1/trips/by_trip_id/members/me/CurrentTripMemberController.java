package com.earthtrip.identity.adapter.in.web.api.v1.trips.by_trip_id.members.me;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.TripMemberUseCase;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/members/me")
class CurrentTripMemberController {
    private final TripMemberUseCase useCase;
    private final CurrentUserProvider currentUser;

    CurrentTripMemberController(TripMemberUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID tripId) {
        useCase.leave(tripId, currentUser.requireUserId());
    }
}
