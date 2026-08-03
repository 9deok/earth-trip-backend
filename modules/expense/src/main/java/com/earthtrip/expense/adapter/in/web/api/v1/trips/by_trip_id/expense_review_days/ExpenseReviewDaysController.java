package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.expense_review_days;

import com.earthtrip.expense.application.port.in.ExpenseReviewUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/expense-review-days")
class ExpenseReviewDaysController {

    private final ExpenseReviewUseCase useCase;
    private final CurrentActor actor;

    ExpenseReviewDaysController(ExpenseReviewUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<ExpenseReviewUseCase.ReviewDayResult> get(@PathVariable UUID tripId) {
        return useCase.list(tripId, actor.requireUserId());
    }
}
