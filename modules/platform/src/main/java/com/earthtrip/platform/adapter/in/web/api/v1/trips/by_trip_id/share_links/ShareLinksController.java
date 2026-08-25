package com.earthtrip.platform.adapter.in.web.api.v1.trips.by_trip_id.share_links;

import com.earthtrip.platform.application.port.in.TripShareManagementUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/share-links")
class ShareLinksController {

    private final TripShareManagementUseCase useCase;
    private final CurrentActor actor;

    ShareLinksController(TripShareManagementUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<TripShareManagementUseCase.ShareLinkResult> get(@PathVariable UUID tripId) {
        return useCase.list(tripId, actor.requireUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TripShareManagementUseCase.ShareLinkResult post(
            @PathVariable UUID tripId, @Valid @RequestBody ShareLinkRequest request) {
        return useCase.create(tripId, actor.requireUserId(), request.toCommand());
    }
}

record ShareLinkRequest(
        @NotNull UUID requestId,
        @Size(min = 1, max = 120) String name,
        @NotNull @Size(min = 1, max = 4) List<String> scopes,
        @Size(min = 4, max = 128) String password,
        @Pattern(regexp = "LINK_ONLY|PUBLIC") String visibility,
        @Size(max = 500) String publicNote,
        @Size(max = 9) Map<String, String> publicContent,
        Instant expiresAt) {
    TripShareManagementUseCase.ShareLinkCommand toCommand() {
        return new TripShareManagementUseCase.ShareLinkCommand(
                requestId,
                name,
                scopes,
                password,
                false,
                visibility,
                publicNote,
                publicContent,
                expiresAt,
                false,
                0);
    }
}
