package com.earthtrip.platform.adapter.in.web.api.v1.trips.by_trip_id.reservations.by_reservation_id.files.by_file_id;

import com.earthtrip.platform.application.port.in.FileUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/reservations/{reservationId}/files/{fileId}")
class ReservationFileByIdController {

    private final FileUseCase useCase;
    private final CurrentActor actor;

    ReservationFileByIdController(FileUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
        @PathVariable UUID tripId,
        @PathVariable UUID reservationId,
        @PathVariable UUID fileId
    ) {
        useCase.unlinkResourceFile(
            actor.requireUserId(), tripId, "RESERVATION", reservationId, fileId
        );
    }
}
