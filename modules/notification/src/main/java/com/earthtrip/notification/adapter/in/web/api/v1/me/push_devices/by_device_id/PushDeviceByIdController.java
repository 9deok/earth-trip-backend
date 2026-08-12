package com.earthtrip.notification.adapter.in.web.api.v1.me.push_devices.by_device_id;

import com.earthtrip.notification.application.port.in.NotificationUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/push-devices/{deviceId}")
class PushDeviceByIdController {

    private final NotificationUseCase useCase;
    private final CurrentActor actor;

    PushDeviceByIdController(NotificationUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    NotificationUseCase.DeviceResult post(
            @PathVariable String deviceId, @Valid @RequestBody PushDeviceMutation request) {
        return useCase.registerDevice(
                actor.requireUserId(),
                deviceId,
                request.platform(),
                request.token(),
                request.appBuild());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String deviceId) {
        useCase.removeDevice(actor.requireUserId(), deviceId);
    }
}

record PushDeviceMutation(
        @NotBlank String platform, @NotBlank String token, @Positive int appBuild) {}
