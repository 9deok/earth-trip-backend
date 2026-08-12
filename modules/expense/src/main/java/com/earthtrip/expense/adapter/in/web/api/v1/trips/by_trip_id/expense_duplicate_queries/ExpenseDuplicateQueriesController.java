package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.expense_duplicate_queries;

import com.earthtrip.expense.application.port.in.ExpenseDuplicateUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/expense-duplicate-queries")
class ExpenseDuplicateQueriesController {

    private final ExpenseDuplicateUseCase useCase;
    private final CurrentActor actor;

    ExpenseDuplicateQueriesController(ExpenseDuplicateUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    List<ExpenseDuplicateUseCase.DuplicateResult> post(
            @PathVariable UUID tripId, @Valid @RequestBody ExpenseDuplicateQueryRequest request) {
        return useCase.query(
                tripId,
                actor.requireUserId(),
                new ExpenseDuplicateUseCase.DuplicateQuery(
                        request.sourceId(),
                        request.title(),
                        request.amountMinor(),
                        request.currency(),
                        request.occurredAt()));
    }
}

record ExpenseDuplicateQueryRequest(
        UUID sourceId,
        @NotBlank @Size(max = 200) String title,
        @Positive long amountMinor,
        @NotBlank String currency,
        @NotNull Instant occurredAt) {}
