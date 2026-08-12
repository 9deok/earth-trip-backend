package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.decisions;

import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/decisions")
class DecisionsController {
    private final PlanningResourceUseCase useCase;
    private final CurrentActor actor;

    DecisionsController(PlanningResourceUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    List<PlanningResourceUseCase.ResourceResult> get(@PathVariable UUID tripId) {
        return useCase.list(tripId, actor.requireUserId(), "DECISION", null, null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PlanningResourceUseCase.ResourceResult post(
            @PathVariable UUID tripId, @Valid @RequestBody DecisionMutation r) {
        if (r.bookingOrPaymentImpact() && !r.impactConfirmed())
            throw new IllegalArgumentException("예약 또는 결제 영향 확인이 필요합니다.");
        Map<String, Object> payload = new LinkedHashMap<>(r.payload());
        payload.put("bookingOrPaymentImpact", r.bookingOrPaymentImpact());
        payload.put("impactConfirmed", r.impactConfirmed());
        return useCase.create(
                tripId,
                actor.requireUserId(),
                "DECISION",
                PlanningResourceUseCase.WritePermission.EDITOR,
                new PlanningResourceUseCase.ResourceCommand(
                        r.requestId(), r.subjectId(), null, payload, "CONFIRMED", 0, 0));
    }
}

record DecisionMutation(
        @NotNull UUID requestId,
        UUID subjectId,
        @NotNull Map<String, Object> payload,
        boolean bookingOrPaymentImpact,
        boolean impactConfirmed) {}
