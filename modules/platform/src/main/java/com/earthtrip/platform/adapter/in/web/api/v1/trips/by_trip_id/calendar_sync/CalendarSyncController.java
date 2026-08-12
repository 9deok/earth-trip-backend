package com.earthtrip.platform.adapter.in.web.api.v1.trips.by_trip_id.calendar_sync;

import com.earthtrip.platform.application.port.in.IntegrationUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/calendar-sync")
class CalendarSyncController {
    private final IntegrationUseCase u;
    private final CurrentActor a;

    CalendarSyncController(IntegrationUseCase u, CurrentActor a) {
        this.u = u;
        this.a = a;
    }

    @GetMapping
    IntegrationUseCase.CalendarSyncResult get(@PathVariable UUID tripId) {
        return u.calendar(tripId, a.requireUserId());
    }

    @PutMapping
    IntegrationUseCase.CalendarSyncResult put(
            @PathVariable UUID tripId, @Valid @RequestBody CalendarRequest r) {
        return u.putCalendar(
                tripId,
                a.requireUserId(),
                new IntegrationUseCase.CalendarCommand(
                        r.connectionId(), r.scopeConfig(), r.baseVersion()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @PathVariable UUID tripId,
            @RequestParam @PositiveOrZero long baseVersion,
            @RequestParam(defaultValue = "false") boolean deleteExternalCalendar) {
        u.deleteCalendar(tripId, a.requireUserId(), baseVersion, deleteExternalCalendar);
    }
}

record CalendarRequest(
        @NotNull UUID connectionId,
        Map<String, Object> scopeConfig,
        @PositiveOrZero long baseVersion) {}
