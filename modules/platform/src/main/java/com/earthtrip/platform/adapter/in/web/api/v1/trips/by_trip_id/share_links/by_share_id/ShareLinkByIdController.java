package com.earthtrip.platform.adapter.in.web.api.v1.trips.by_trip_id.share_links.by_share_id;

import com.earthtrip.platform.application.port.in.TripShareUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/share-links/{shareId}")
class ShareLinkByIdController {

    private final TripShareUseCase useCase;
    private final CurrentActor actor;

    ShareLinkByIdController(TripShareUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PatchMapping
    TripShareUseCase.ShareLinkResult patch(
        @PathVariable UUID tripId,
        @PathVariable UUID shareId,
        @Valid @RequestBody ShareLinkPatchRequest request
    ) {
        return useCase.update(tripId, shareId, actor.requireUserId(), request.toCommand());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
        @PathVariable UUID tripId,
        @PathVariable UUID shareId,
        @RequestParam @PositiveOrZero long baseVersion
    ) {
        useCase.revoke(tripId, shareId, actor.requireUserId(), baseVersion);
    }
}

record ShareLinkPatchRequest(
    @Size(min = 1, max = 120) String name,
    @Size(min = 1, max = 4) List<String> scopes,
    @Size(min = 4, max = 128) String password,
    Boolean removePassword,
    Instant expiresAt,
    Boolean removeExpiry,
    @PositiveOrZero long baseVersion
) {
    TripShareUseCase.ShareLinkCommand toCommand() {
        return new TripShareUseCase.ShareLinkCommand(
            null, name, scopes, password, removePassword, expiresAt, removeExpiry, baseVersion
        );
    }
}
