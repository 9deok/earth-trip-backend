package com.earthtrip.platform.adapter.in.web.api.v1.trips.by_trip_id.research_sources.by_source_id.analysis_jobs;

import com.earthtrip.platform.application.port.in.AnalysisJobUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/research-sources/{sourceId}/analysis-jobs")
class ResearchSourceAnalysisJobsController {
    private final AnalysisJobUseCase useCase;
    private final CurrentActor actor;

    ResearchSourceAnalysisJobsController(AnalysisJobUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    AnalysisJobUseCase.JobResult post(
            @PathVariable UUID tripId,
            @PathVariable UUID sourceId,
            @Valid @RequestBody ResearchAnalysisJobRequest request) {
        return useCase.createResearchSourceJob(
                tripId, sourceId, actor.requireUserId(), request.command());
    }
}

record ResearchAnalysisJobRequest(
        @NotNull UUID requestId,
        @NotNull Map<String, Object> inputPayload,
        @Size(max = 200) List<@Valid ResearchAnalysisSuggestion> suggestions) {
    AnalysisJobUseCase.CreateCommand command() {
        return new AnalysisJobUseCase.CreateCommand(
                requestId,
                inputPayload,
                suggestions == null
                        ? List.of()
                        : suggestions.stream().map(ResearchAnalysisSuggestion::command).toList());
    }
}

record ResearchAnalysisSuggestion(
        @NotBlank String field,
        Object value,
        @DecimalMin("0.0") @DecimalMax("1.0") Double confidence,
        String sourceReference,
        List<String> warnings) {
    AnalysisJobUseCase.SuggestionCommand command() {
        return new AnalysisJobUseCase.SuggestionCommand(
                field, value, confidence, sourceReference, warnings);
    }
}
