package com.earthtrip.platform.adapter.in.web.api.v1.trips.by_trip_id.reservations.by_reservation_id.files;

import com.earthtrip.platform.application.port.in.FileUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
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
@RequestMapping("/api/v1/trips/{tripId}/reservations/{reservationId}/files")
class ReservationFilesController {

    private final FileUseCase useCase;
    private final CurrentActor actor;

    ReservationFilesController(FileUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<ReservationFileResponse> get(@PathVariable UUID tripId, @PathVariable UUID reservationId) {
        return useCase
                .linkedFiles(actor.requireUserId(), tripId, "RESERVATION", reservationId)
                .stream()
                .map(ReservationFileResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    FileUseCase.LinkResult post(
            @PathVariable UUID tripId,
            @PathVariable UUID reservationId,
            @Valid @RequestBody ReservationFileLinkRequest request) {
        return useCase.link(
                actor.requireUserId(),
                request.fileId(),
                request.requestId(),
                tripId,
                "RESERVATION",
                reservationId,
                request.visibility());
    }
}

record ReservationFileLinkRequest(
        @NotNull UUID requestId, @NotNull UUID fileId, String visibility) {}

record ReservationFileResponse(
        UUID fileId,
        String fileName,
        String mimeType,
        long sizeBytes,
        String checksumSha256,
        String status,
        long version,
        Instant createdAt,
        Instant completedAt) {
    static ReservationFileResponse from(FileUseCase.FileResult result) {
        return new ReservationFileResponse(
                result.fileId(),
                result.fileName(),
                result.mimeType(),
                result.sizeBytes(),
                result.checksumSha256(),
                result.status(),
                result.version(),
                result.createdAt(),
                result.completedAt());
    }
}
