package com.earthtrip.wallet.adapter.in.web.api.v1.reservation_import_jobs.by_job_id.retries;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.ReservationImportUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservation-import-jobs/{jobId}/retries")
class ReservationImportRetriesController {

    private final ReservationImportUseCase useCase;
    private final CurrentActor actor;

    ReservationImportRetriesController(ReservationImportUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    ReservationImportUseCase.ImportResult retry(
        @PathVariable UUID jobId,
        @Valid @RequestBody ReservationImportRetryRequest request
    ) {
        return useCase.retry(jobId, actor.requireUserId(), request.baseVersion());
    }
}

record ReservationImportRetryRequest(@PositiveOrZero long baseVersion) { }
