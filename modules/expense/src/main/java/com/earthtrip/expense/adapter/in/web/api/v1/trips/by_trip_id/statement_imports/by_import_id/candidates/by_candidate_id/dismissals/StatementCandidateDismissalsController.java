package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.statement_imports.by_import_id.candidates.by_candidate_id.dismissals;

import com.earthtrip.expense.application.port.in.StatementImportUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    "/api/v1/trips/{tripId}/statement-imports/{importId}/candidates/{candidateId}/dismissals"
)
class StatementCandidateDismissalsController {

    private final StatementImportUseCase useCase;
    private final CurrentActor actor;

    StatementCandidateDismissalsController(
        StatementImportUseCase useCase,
        CurrentActor actor
    ) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    StatementImportUseCase.CandidateResult post(
        @PathVariable UUID tripId,
        @PathVariable UUID importId,
        @PathVariable UUID candidateId,
        @Valid @RequestBody StatementCandidateDismissalRequest request
    ) {
        return useCase.dismiss(
            tripId, importId, candidateId, actor.requireUserId(),
            request.reason(), request.baseVersion()
        );
    }
}

record StatementCandidateDismissalRequest(
    @Size(max = 500) String reason,
    @PositiveOrZero long baseVersion
) { }
