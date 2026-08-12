package com.earthtrip.platform.adapter.in.web.api.v1.trips.by_trip_id.calendar_sync.runs.by_run_id;

import com.earthtrip.platform.application.port.in.IntegrationUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/calendar-sync/runs/{runId}")
class CalendarSyncRunByIdController {
    private final IntegrationUseCase u;
    private final CurrentActor a;

    CalendarSyncRunByIdController(IntegrationUseCase u, CurrentActor a) {
        this.u = u;
        this.a = a;
    }

    @GetMapping
    IntegrationUseCase.SyncJobResult get(@PathVariable UUID tripId, @PathVariable UUID runId) {
        return u.calendarRun(tripId, runId, a.requireUserId());
    }
}
