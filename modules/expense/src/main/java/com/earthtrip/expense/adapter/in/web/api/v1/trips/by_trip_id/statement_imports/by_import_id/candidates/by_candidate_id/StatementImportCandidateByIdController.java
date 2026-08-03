package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.statement_imports.by_import_id.candidates.by_candidate_id;

import com.earthtrip.expense.application.port.in.StatementImportUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    "/api/v1/trips/{tripId}/statement-imports/{importId}/candidates/{candidateId}"
)
class StatementImportCandidateByIdController {

    private final StatementImportUseCase useCase;
    private final CurrentActor actor;

    StatementImportCandidateByIdController(
        StatementImportUseCase useCase,
        CurrentActor actor
    ) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PatchMapping
    StatementImportUseCase.CandidateResult patch(
        @PathVariable UUID tripId,
        @PathVariable UUID importId,
        @PathVariable UUID candidateId,
        @Valid @RequestBody StatementCandidatePatchRequest request
    ) {
        return useCase.updateCandidate(
            tripId, importId, candidateId, actor.requireUserId(),
            new StatementImportUseCase.CandidateUpdate(
                request.title(), request.amountMinor(), request.currency(),
                request.occurredAt(), request.payerUserId(), request.payload(),
                request.baseVersion()
            )
        );
    }
}

record StatementCandidatePatchRequest(
    @Size(min = 1, max = 200) String title,
    @Positive Long amountMinor,
    String currency,
    Instant occurredAt,
    UUID payerUserId,
    Map<String, Object> payload,
    @PositiveOrZero long baseVersion
) { }
