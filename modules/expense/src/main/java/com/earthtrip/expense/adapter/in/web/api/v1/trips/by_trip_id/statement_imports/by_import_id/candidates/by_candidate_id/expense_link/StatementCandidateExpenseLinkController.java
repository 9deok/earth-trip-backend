package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.statement_imports.by_import_id.candidates.by_candidate_id.expense_link;

import com.earthtrip.expense.application.port.in.StatementImportUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "/api/v1/trips/{tripId}/statement-imports/{importId}/candidates/{candidateId}/expense-link")
class StatementCandidateExpenseLinkController {

    private final StatementImportUseCase useCase;
    private final CurrentActor actor;

    StatementCandidateExpenseLinkController(StatementImportUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PutMapping
    StatementImportUseCase.CandidateResult put(
            @PathVariable UUID tripId,
            @PathVariable UUID importId,
            @PathVariable UUID candidateId,
            @Valid @RequestBody StatementCandidateExpenseLinkRequest request) {
        return useCase.linkExpense(
                tripId,
                importId,
                candidateId,
                actor.requireUserId(),
                request.expenseId(),
                request.baseVersion());
    }

    @DeleteMapping
    StatementImportUseCase.CandidateResult delete(
            @PathVariable UUID tripId,
            @PathVariable UUID importId,
            @PathVariable UUID candidateId,
            @RequestParam @PositiveOrZero long baseVersion) {
        return useCase.unlinkExpense(
                tripId, importId, candidateId, actor.requireUserId(), baseVersion);
    }
}

record StatementCandidateExpenseLinkRequest(
        @NotNull UUID expenseId, @PositiveOrZero long baseVersion) {}
