package com.earthtrip.notification.adapter.in.web.api.v1.me.notification_summary;

import com.earthtrip.notification.application.port.in.NotificationUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/notification-summary")
class NotificationSummaryController {
    private final NotificationUseCase useCase;
    private final CurrentActor actor;

    NotificationSummaryController(NotificationUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    NotificationUseCase.SummaryResult get() {
        return useCase.summary(actor.requireUserId());
    }
}
