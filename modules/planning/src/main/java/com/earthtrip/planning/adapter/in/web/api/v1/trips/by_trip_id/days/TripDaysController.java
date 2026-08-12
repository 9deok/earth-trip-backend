package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.days;

import com.earthtrip.planning.application.port.in.TripDayUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.time.LocalDate;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/days")
class TripDaysController {
    private final TripDayUseCase useCase;
    private final CurrentActor actor;

    TripDaysController(TripDayUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    List<DayResponse> get(@PathVariable UUID tripId) {
        return useCase.list(tripId, actor.requireUserId()).stream()
                .map(d -> new DayResponse(d.dayId(), d.localDate(), d.dayNumber(), d.timeZone()))
                .toList();
    }
}

record DayResponse(UUID dayId, LocalDate localDate, int dayNumber, String timeZone) {}
