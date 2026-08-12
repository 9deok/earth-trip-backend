package com.earthtrip.wallet.adapter.in.web.api.v1.reservation_import_jobs.by_job_id.cancellations;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.ReservationImportUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservation-import-jobs/{jobId}/cancellations")
class ReservationImportCancellationsController {

    private final ReservationImportUseCase useCase;
    private final CurrentActor actor;

    ReservationImportCancellationsController(ReservationImportUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    ReservationImportUseCase.ImportResult cancel(
            @PathVariable UUID jobId,
            @Valid @RequestBody ReservationImportCancellationRequest request) {
        return useCase.cancel(jobId, actor.requireUserId(), request.baseVersion());
    }
}

record ReservationImportCancellationRequest(@PositiveOrZero long baseVersion) {}
