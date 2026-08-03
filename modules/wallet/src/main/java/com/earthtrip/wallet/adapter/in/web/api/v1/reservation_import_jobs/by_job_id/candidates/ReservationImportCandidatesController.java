package com.earthtrip.wallet.adapter.in.web.api.v1.reservation_import_jobs.by_job_id.candidates;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.ReservationImportUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservation-import-jobs/{jobId}/candidates")
class ReservationImportCandidatesController {

    private final ReservationImportUseCase useCase;
    private final CurrentActor actor;

    ReservationImportCandidatesController(ReservationImportUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<ReservationImportUseCase.CandidateResult> get(@PathVariable UUID jobId) {
        return useCase.candidates(jobId, actor.requireUserId());
    }
}
