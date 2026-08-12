package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.expense_categories.by_category_id;

import com.earthtrip.expense.application.port.in.ExpenseCategoryUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/expense-categories/{categoryId}")
class ExpenseCategoryByIdController {

    private final ExpenseCategoryUseCase useCase;
    private final CurrentActor actor;

    ExpenseCategoryByIdController(ExpenseCategoryUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    ExpenseCategoryUseCase.CategoryResult get(
            @PathVariable UUID tripId, @PathVariable UUID categoryId) {
        return useCase.get(tripId, categoryId, actor.requireUserId());
    }

    @PatchMapping
    ExpenseCategoryUseCase.CategoryResult patch(
            @PathVariable UUID tripId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody ExpenseCategoryUpdateRequest request) {
        return useCase.update(
                tripId,
                categoryId,
                actor.requireUserId(),
                request.name(),
                request.color(),
                request.sortOrder(),
                request.baseVersion());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @PathVariable UUID tripId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody ExpenseCategoryDeleteRequest request) {
        useCase.delete(
                tripId,
                categoryId,
                actor.requireUserId(),
                request.replacementCode(),
                request.baseVersion());
    }
}

record ExpenseCategoryUpdateRequest(
        @Size(min = 1, max = 100) String name,
        @Pattern(regexp = "#[0-9A-Fa-f]{6}") String color,
        @PositiveOrZero Integer sortOrder,
        @PositiveOrZero long baseVersion) {}

record ExpenseCategoryDeleteRequest(String replacementCode, @PositiveOrZero long baseVersion) {}
