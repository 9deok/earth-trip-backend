package com.earthtrip.notification.adapter.in.web.internal.admin.push_notifications;

import com.earthtrip.notification.api.NotificationPublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin/push-notifications")
class AdminPushNotificationsController {

    private final NotificationPublisher publisher;

    AdminPushNotificationsController(NotificationPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    NotificationPublisher.PublishResult post(@Valid @RequestBody PushNotificationRequest request) {
        return publisher.publish(new NotificationPublisher.PublishCommand(
            request.notificationId(),
            request.userId(),
            request.tripId(),
            request.type(),
            request.title(),
            request.body(),
            request.deepLink(),
            request.metadata() == null ? Map.of() : request.metadata()
        ));
    }
}

record PushNotificationRequest(
    UUID notificationId,
    @NotNull UUID userId,
    UUID tripId,
    @NotBlank String type,
    @NotBlank String title,
    @NotBlank String body,
    String deepLink,
    Map<String, Object> metadata
) { }
