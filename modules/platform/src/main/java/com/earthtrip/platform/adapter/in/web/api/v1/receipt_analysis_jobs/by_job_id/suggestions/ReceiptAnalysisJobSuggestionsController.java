package com.earthtrip.platform.adapter.in.web.api.v1.receipt_analysis_jobs.by_job_id.suggestions;

import com.earthtrip.platform.application.port.in.AnalysisJobUseCase;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/receipt-analysis-jobs/{jobId}/suggestions")
class ReceiptAnalysisJobSuggestionsController {
    private final AnalysisJobUseCase useCase;
    private final CurrentActor actor;

    ReceiptAnalysisJobSuggestionsController(AnalysisJobUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<AnalysisJobUseCase.SuggestionResult> get(@PathVariable UUID jobId) {
        AnalysisJobUseCase.JobResult job = useCase.get(jobId, actor.requireUserId());
        if (!job.targetType().equals("EXPENSE_RECEIPT")) {
            throw EarthTripException.notFound("RECEIPT_ANALYSIS_NOT_FOUND", "영수증 분석을 찾을 수 없습니다.");
        }
        return useCase.suggestions(jobId, actor.requireUserId());
    }
}
