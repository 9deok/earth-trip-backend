package com.earthtrip.notification.adapter.in.web.api.v1.me.notification_preferences;

import com.earthtrip.notification.application.port.in.NotificationUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.time.LocalTime;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/notification-preferences")
class NotificationPreferencesController {
    private final NotificationUseCase useCase;
    private final CurrentActor actor;

    NotificationPreferencesController(NotificationUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    NotificationUseCase.PreferenceResult get() {
        return useCase.preferences(actor.requireUserId());
    }

    @PatchMapping
    NotificationUseCase.PreferenceResult patch(@RequestBody PreferenceMutation r) {
        return useCase.updatePreferences(
                actor.requireUserId(),
                new NotificationUseCase.PreferenceCommand(
                        r.mentionsEnabled(),
                        r.scheduleEnabled(),
                        r.expenseEnabled(),
                        r.invitationEnabled(),
                        r.pushEnabled(),
                        r.emailEnabled(),
                        r.quietStart(),
                        r.quietEnd(),
                        r.quietTimeZone()));
    }
}

record PreferenceMutation(
        Boolean mentionsEnabled,
        Boolean scheduleEnabled,
        Boolean expenseEnabled,
        Boolean invitationEnabled,
        Boolean pushEnabled,
        Boolean emailEnabled,
        LocalTime quietStart,
        LocalTime quietEnd,
        String quietTimeZone) {}
