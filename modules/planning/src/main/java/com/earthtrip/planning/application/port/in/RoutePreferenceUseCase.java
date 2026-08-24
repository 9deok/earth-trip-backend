package com.earthtrip.planning.application.port.in;

import java.util.List;
import java.util.UUID;

public interface RoutePreferenceUseCase {

    PreferenceResult get(UUID tripId, UUID actorUserId);

    PreferenceResult update(UUID tripId, UUID actorUserId, PreferenceCommand command);

    record PreferenceCommand(
            List<String> allowedModes,
            Integer maximumWalkingMinutes,
            Integer defaultBufferMinutes,
            Boolean startAtAccommodation,
            Boolean endAtAccommodation,
            Boolean avoidTolls,
            Boolean accessibilityRequired,
            long baseVersion) {}

    record PreferenceResult(
            List<String> allowedModes,
            int maximumWalkingMinutes,
            int defaultBufferMinutes,
            boolean startAtAccommodation,
            boolean endAtAccommodation,
            boolean avoidTolls,
            boolean accessibilityRequired,
            long version,
            boolean configured) {}
}
