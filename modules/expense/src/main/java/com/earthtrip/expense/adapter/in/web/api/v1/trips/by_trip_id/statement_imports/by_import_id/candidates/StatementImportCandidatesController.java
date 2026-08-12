package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.statement_imports.by_import_id.candidates;

import com.earthtrip.expense.application.port.in.StatementImportUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/statement-imports/{importId}/candidates")
class StatementImportCandidatesController {

    private final StatementImportUseCase useCase;
    private final CurrentActor actor;

    StatementImportCandidatesController(StatementImportUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<StatementImportUseCase.CandidateResult> get(
            @PathVariable UUID tripId, @PathVariable UUID importId) {
        return useCase.candidates(tripId, importId, actor.requireUserId());
    }
}
