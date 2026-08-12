package com.earthtrip.platform.adapter.in.web.api.v1.analysis_jobs.by_job_id.retries;

import com.earthtrip.platform.application.port.in.AnalysisJobUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analysis-jobs/{jobId}/retries")
class AnalysisJobRetriesController {
    private final AnalysisJobUseCase useCase;
    private final CurrentActor actor;

    AnalysisJobRetriesController(AnalysisJobUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    AnalysisJobUseCase.JobResult post(
            @PathVariable UUID jobId, @RequestParam @PositiveOrZero long baseVersion) {
        return useCase.retry(jobId, actor.requireUserId(), baseVersion);
    }
}
