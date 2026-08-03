package com.earthtrip.wallet.adapter.in.web.api.v1.reservation_import_jobs.by_job_id.confirmations;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.ReservationImportUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservation-import-jobs/{jobId}/confirmations")
class ReservationImportConfirmationsController {

    private final ReservationImportUseCase useCase;
    private final CurrentActor actor;

    ReservationImportConfirmationsController(
        ReservationImportUseCase useCase,
        CurrentActor actor
    ) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    ReservationImportUseCase.ConfirmationResult confirm(
        @PathVariable UUID jobId,
        @Valid @RequestBody ReservationImportConfirmationRequest request
    ) {
        return useCase.confirm(
            jobId,
            actor.requireUserId(),
            request.items().stream().map(ReservationImportConfirmationItem::command).toList()
        );
    }
}

record ReservationImportConfirmationRequest(
    @NotEmpty List<@Valid ReservationImportConfirmationItem> items
) { }

record ReservationImportConfirmationItem(
    @NotNull UUID candidateId,
    @NotNull UUID reservationRequestId,
    Map<String, Object> payloadOverride,
    String visibility,
    @PositiveOrZero Integer sortOrder,
    @PositiveOrZero long baseVersion
) {
    ReservationImportUseCase.ConfirmationItem command() {
        return new ReservationImportUseCase.ConfirmationItem(
            candidateId, reservationRequestId, payloadOverride, visibility, sortOrder, baseVersion
        );
    }
}
