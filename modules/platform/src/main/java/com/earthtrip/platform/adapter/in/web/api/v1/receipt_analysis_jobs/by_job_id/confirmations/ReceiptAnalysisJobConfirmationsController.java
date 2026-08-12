package com.earthtrip.platform.adapter.in.web.api.v1.receipt_analysis_jobs.by_job_id.confirmations;

import com.earthtrip.platform.application.port.in.AnalysisJobUseCase;
import com.earthtrip.sharedkernel.error.EarthTripException;
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
@RequestMapping("/api/v1/receipt-analysis-jobs/{jobId}/confirmations")
class ReceiptAnalysisJobConfirmationsController {
    private final AnalysisJobUseCase useCase;
    private final CurrentActor actor;

    ReceiptAnalysisJobConfirmationsController(AnalysisJobUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    AnalysisJobUseCase.ConfirmationResult post(
            @PathVariable UUID jobId,
            @Valid @RequestBody ReceiptAnalysisConfirmationRequest request) {
        UUID actorId = actor.requireUserId();
        AnalysisJobUseCase.JobResult job = useCase.get(jobId, actorId);
        if (!job.targetType().equals("EXPENSE_RECEIPT")) {
            throw EarthTripException.notFound("RECEIPT_ANALYSIS_NOT_FOUND", "영수증 분석을 찾을 수 없습니다.");
        }
        return useCase.confirm(jobId, actorId, request.command());
    }
}

record ReceiptAnalysisConfirmationRequest(
        @NotNull UUID requestId,
        @NotNull Map<String, Object> confirmedFields,
        @PositiveOrZero long targetBaseVersion,
        @PositiveOrZero long jobBaseVersion) {
    AnalysisJobUseCase.ConfirmationCommand command() {
        return new AnalysisJobUseCase.ConfirmationCommand(
                requestId, confirmedFields, targetBaseVersion, jobBaseVersion);
    }
}
