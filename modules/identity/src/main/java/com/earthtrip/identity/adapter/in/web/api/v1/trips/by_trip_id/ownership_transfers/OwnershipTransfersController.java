package com.earthtrip.identity.adapter.in.web.api.v1.trips.by_trip_id.ownership_transfers;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.TripMemberUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/ownership-transfers")
class OwnershipTransfersController {
    private final TripMemberUseCase useCase;
    private final CurrentUserProvider currentUser;

    OwnershipTransfersController(TripMemberUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void post(@PathVariable UUID tripId, @Valid @RequestBody OwnershipTransferRequest request) {
        useCase.transferOwnership(
                tripId, currentUser.requireUserId(), request.toMemberId(), request.confirmed());
    }
}

record OwnershipTransferRequest(@NotNull UUID toMemberId, @AssertTrue boolean confirmed) {}
