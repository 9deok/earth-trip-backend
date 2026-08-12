package com.earthtrip.identity.adapter.in.web.api.v1.me.devices;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.SessionUseCase;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/devices")
class DevicesController {

    private final SessionUseCase useCase;
    private final CurrentUserProvider currentUser;

    DevicesController(SessionUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<DeviceResponse> get() {
        return useCase.list(currentUser.requireUserId(), currentUser.requireSessionId()).stream()
                .map(
                        session ->
                                new DeviceResponse(
                                        session.sessionId(),
                                        session.deviceName(),
                                        session.current(),
                                        session.active(),
                                        session.lastUsedAt(),
                                        session.createdAt()))
                .toList();
    }
}

record DeviceResponse(
        UUID sessionId,
        String deviceName,
        boolean current,
        boolean active,
        Instant lastUsedAt,
        Instant createdAt) {}
