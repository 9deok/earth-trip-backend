package com.earthtrip.platform.adapter.in.web.api.v1.trips.by_trip_id.calendar_sync.runs;

import com.earthtrip.platform.application.port.in.IntegrationUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/calendar-sync/runs")
class CalendarSyncRunsController {
    private final IntegrationUseCase u;
    private final CurrentActor a;

    CalendarSyncRunsController(IntegrationUseCase u, CurrentActor a) {
        this.u = u;
        this.a = a;
    }

    @PostMapping
    IntegrationUseCase.SyncJobResult post(
            @PathVariable UUID tripId, @Valid @RequestBody CalendarRunRequest r) {
        return u.runCalendar(tripId, a.requireUserId(), r.requestId());
    }
}

record CalendarRunRequest(@NotNull UUID requestId) {}
