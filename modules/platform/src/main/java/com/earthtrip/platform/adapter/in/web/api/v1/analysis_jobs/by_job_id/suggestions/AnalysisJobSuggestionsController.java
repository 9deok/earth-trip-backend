package com.earthtrip.platform.adapter.in.web.api.v1.analysis_jobs.by_job_id.suggestions;

import com.earthtrip.platform.application.port.in.AnalysisJobUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analysis-jobs/{jobId}/suggestions")
class AnalysisJobSuggestionsController {
    private final AnalysisJobUseCase useCase;
    private final CurrentActor actor;

    AnalysisJobSuggestionsController(AnalysisJobUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<AnalysisJobUseCase.SuggestionResult> get(@PathVariable UUID jobId) {
        return useCase.suggestions(jobId, actor.requireUserId());
    }
}
