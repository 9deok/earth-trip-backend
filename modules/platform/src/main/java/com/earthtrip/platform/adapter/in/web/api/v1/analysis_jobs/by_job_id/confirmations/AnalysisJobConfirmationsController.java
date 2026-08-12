package com.earthtrip.platform.adapter.in.web.api.v1.analysis_jobs.by_job_id.confirmations;

import com.earthtrip.platform.application.port.in.AnalysisJobUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analysis-jobs/{jobId}/confirmations")
class AnalysisJobConfirmationsController {
    private final AnalysisJobUseCase useCase;
    private final CurrentActor actor;

    AnalysisJobConfirmationsController(AnalysisJobUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    AnalysisJobUseCase.ConfirmationResult post(
            @PathVariable UUID jobId, @Valid @RequestBody AnalysisConfirmationRequest request) {
        return useCase.confirm(jobId, actor.requireUserId(), request.command());
    }
}

record AnalysisConfirmationRequest(
        @NotNull UUID requestId,
        @NotNull Map<String, Object> confirmedFields,
        @PositiveOrZero long targetBaseVersion,
        @PositiveOrZero long jobBaseVersion) {
    AnalysisJobUseCase.ConfirmationCommand command() {
        return new AnalysisJobUseCase.ConfirmationCommand(
                requestId, confirmedFields, targetBaseVersion, jobBaseVersion);
    }
}
