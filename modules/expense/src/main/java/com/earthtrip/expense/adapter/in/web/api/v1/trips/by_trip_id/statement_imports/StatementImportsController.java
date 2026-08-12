package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.statement_imports;

import com.earthtrip.expense.application.port.in.StatementImportUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/statement-imports")
class StatementImportsController {

    private final StatementImportUseCase useCase;
    private final CurrentActor actor;

    StatementImportsController(StatementImportUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<StatementImportUseCase.ImportResult> get(@PathVariable UUID tripId) {
        return useCase.list(tripId, actor.requireUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    StatementImportUseCase.ImportResult post(
            @PathVariable UUID tripId, @Valid @RequestBody StatementImportRequest request) {
        return useCase.create(tripId, actor.requireUserId(), request.toCommand());
    }
}

record StatementImportRequest(
        @NotNull UUID requestId,
        @NotBlank @Size(max = 80) String source,
        @NotNull @Size(min = 1, max = 500) @Valid List<StatementCandidateRequest> candidates) {
    StatementImportUseCase.ImportCommand toCommand() {
        return new StatementImportUseCase.ImportCommand(
                requestId,
                source,
                candidates.stream().map(StatementCandidateRequest::toCommand).toList());
    }
}

record StatementCandidateRequest(
        @NotNull UUID candidateId,
        @NotBlank @Size(max = 200) String title,
        @Positive long amountMinor,
        @NotBlank String currency,
        @NotNull Instant occurredAt,
        @NotNull UUID payerUserId,
        Map<String, Object> payload) {
    StatementImportUseCase.CandidateCommand toCommand() {
        return new StatementImportUseCase.CandidateCommand(
                candidateId, title, amountMinor, currency, occurredAt, payerUserId, payload);
    }
}
