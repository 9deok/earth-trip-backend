package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.expense_categories;

import com.earthtrip.expense.application.port.in.ExpenseCategoryUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
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
@RequestMapping("/api/v1/trips/{tripId}/expense-categories")
class ExpenseCategoriesController {

    private final ExpenseCategoryUseCase useCase;
    private final CurrentActor actor;

    ExpenseCategoriesController(ExpenseCategoryUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<ExpenseCategoryUseCase.CategoryResult> get(@PathVariable UUID tripId) {
        return useCase.list(tripId, actor.requireUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ExpenseCategoryUseCase.CategoryResult post(
            @PathVariable UUID tripId, @Valid @RequestBody ExpenseCategoryCreateRequest request) {
        return useCase.create(
                tripId,
                actor.requireUserId(),
                request.requestId(),
                request.name(),
                request.color(),
                request.sortOrder());
    }
}

record ExpenseCategoryCreateRequest(
        @NotNull UUID requestId,
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "#[0-9A-Fa-f]{6}") String color,
        @PositiveOrZero Integer sortOrder) {}
