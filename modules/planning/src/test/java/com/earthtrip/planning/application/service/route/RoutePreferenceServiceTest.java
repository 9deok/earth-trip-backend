package com.earthtrip.planning.application.service.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.planning.application.port.in.RoutePreferenceUseCase;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoutePreferenceServiceTest {

    @Test
    void 저장된_설정이_없으면_기본값과_미설정_상태를_반환한다() {
        PlanningResourceUseCase resources = mock(PlanningResourceUseCase.class);
        UUID tripId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(resources.list(tripId, actorId, "ROUTE_PREFERENCE", null, null)).thenReturn(List.of());

        RoutePreferenceUseCase.PreferenceResult result =
                new RoutePreferenceService(resources).get(tripId, actorId);

        assertThat(result.version()).isZero();
        assertThat(result.configured()).isFalse();
    }

    @Test
    void 최초_저장도_버전_0과_설정됨_상태를_구분해_반환한다() {
        PlanningResourceUseCase resources = mock(PlanningResourceUseCase.class);
        UUID tripId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(resources.list(tripId, actorId, "ROUTE_PREFERENCE", null, null)).thenReturn(List.of());
        when(resources.create(
                        eq(tripId),
                        eq(actorId),
                        eq("ROUTE_PREFERENCE"),
                        eq(PlanningResourceUseCase.WritePermission.EDITOR),
                        any(PlanningResourceUseCase.ResourceCommand.class)))
                .thenReturn(resource(tripId, actorId));
        RoutePreferenceUseCase.PreferenceCommand command =
                new RoutePreferenceUseCase.PreferenceCommand(
                        List.of("WALKING", "TRANSIT"), 45, 20, true, true, false, true, 0);

        RoutePreferenceUseCase.PreferenceResult result =
                new RoutePreferenceService(resources).update(tripId, actorId, command);

        assertThat(result.version()).isZero();
        assertThat(result.configured()).isTrue();
        assertThat(result.maximumWalkingMinutes()).isEqualTo(45);
    }

    private static PlanningResourceUseCase.ResourceResult resource(UUID tripId, UUID actorId) {
        Instant now = Instant.parse("2026-08-24T00:00:00Z");
        return new PlanningResourceUseCase.ResourceResult(
                UUID.randomUUID(),
                tripId,
                "ROUTE_PREFERENCE",
                null,
                null,
                Map.of(
                        "allowedModes", List.of("WALKING", "TRANSIT"),
                        "maximumWalkingMinutes", 45,
                        "defaultBufferMinutes", 20,
                        "startAtAccommodation", true,
                        "endAtAccommodation", true,
                        "avoidTolls", false,
                        "accessibilityRequired", true),
                "ACTIVE",
                0,
                List.of(),
                0,
                actorId,
                actorId,
                now,
                now);
    }
}
