package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.statement_imports.by_import_id.confirmations;

import com.earthtrip.expense.application.port.in.StatementImportUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
@RequestMapping("/api/v1/trips/{tripId}/statement-imports/{importId}/confirmations")
class StatementImportConfirmationsController {

    private final StatementImportUseCase useCase;
    private final CurrentActor actor;

    StatementImportConfirmationsController(
        StatementImportUseCase useCase,
        CurrentActor actor
    ) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    StatementImportUseCase.ConfirmationResult post(
        @PathVariable UUID tripId,
        @PathVariable UUID importId,
        @Valid @RequestBody StatementImportConfirmationRequest request
    ) {
        return useCase.confirm(
            tripId, importId, actor.requireUserId(),
            request.items().stream().map(StatementConfirmationItemRequest::toItem).toList()
        );
    }
}

record StatementImportConfirmationRequest(
    @NotNull @Size(min = 1, max = 500) @Valid List<StatementConfirmationItemRequest> items
) { }

record StatementConfirmationItemRequest(
    @NotNull UUID candidateId,
    @NotNull UUID expenseRequestId,
    @NotBlank String categoryCode,
    @NotNull Map<UUID, @Positive Long> participantShares,
    String visibility,
    @PositiveOrZero long baseVersion
) {
    StatementImportUseCase.ConfirmationItem toItem() {
        return new StatementImportUseCase.ConfirmationItem(
            candidateId, expenseRequestId, categoryCode, participantShares,
            visibility, baseVersion
        );
    }
}
