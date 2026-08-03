package com.earthtrip.wallet.adapter.in.web.api.v1.reservation_import_jobs.by_job_id.dismissals;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.ReservationImportUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservation-import-jobs/{jobId}/dismissals")
class ReservationImportDismissalsController {

    private final ReservationImportUseCase useCase;
    private final CurrentActor actor;

    ReservationImportDismissalsController(ReservationImportUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    ReservationImportUseCase.ImportResult dismiss(
        @PathVariable UUID jobId,
        @Valid @RequestBody ReservationImportDismissalRequest request
    ) {
        return useCase.dismiss(
            jobId,
            actor.requireUserId(),
            request.items().stream().map(ReservationImportDismissalItem::command).toList()
        );
    }
}

record ReservationImportDismissalRequest(
    @NotEmpty List<@Valid ReservationImportDismissalItem> items
) { }

record ReservationImportDismissalItem(
    @NotNull UUID candidateId,
    @Size(max = 500) String reason,
    @PositiveOrZero long baseVersion
) {
    ReservationImportUseCase.DismissalItem command() {
        return new ReservationImportUseCase.DismissalItem(candidateId, reason, baseVersion);
    }
}
