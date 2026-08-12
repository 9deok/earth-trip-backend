package com.earthtrip.notification.adapter.in.web.api.v1.me.notifications.by_notification_id;

import com.earthtrip.notification.application.port.in.NotificationUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/notifications/{notificationId}")
class NotificationByIdController {
    private final NotificationUseCase useCase;
    private final CurrentActor actor;

    NotificationByIdController(NotificationUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID notificationId) {
        useCase.hide(actor.requireUserId(), notificationId);
    }
}
