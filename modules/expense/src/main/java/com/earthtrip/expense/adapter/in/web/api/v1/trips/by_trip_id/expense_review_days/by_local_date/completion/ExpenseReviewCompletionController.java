package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.expense_review_days.by_local_date.completion;

import com.earthtrip.expense.application.port.in.ExpenseReviewUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    "/api/v1/trips/{tripId}/expense-review-days/{localDate}/completion"
)
class ExpenseReviewCompletionController {

    private final ExpenseReviewUseCase useCase;
    private final CurrentActor actor;

    ExpenseReviewCompletionController(ExpenseReviewUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PutMapping
    ExpenseReviewUseCase.ReviewDayResult put(
        @PathVariable UUID tripId,
        @PathVariable LocalDate localDate,
        @Valid @RequestBody ExpenseReviewCompletionRequest request
    ) {
        return useCase.complete(
            tripId, localDate, actor.requireUserId(), request.note(), request.baseVersion()
        );
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
        @PathVariable UUID tripId,
        @PathVariable LocalDate localDate,
        @RequestParam @PositiveOrZero long baseVersion
    ) {
        useCase.reopen(tripId, localDate, actor.requireUserId(), baseVersion);
    }
}

record ExpenseReviewCompletionRequest(
    @Size(max = 1000) String note,
    @PositiveOrZero long baseVersion
) { }
