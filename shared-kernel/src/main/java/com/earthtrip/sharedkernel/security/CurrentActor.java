package com.earthtrip.sharedkernel.security;

import com.earthtrip.sharedkernel.error.EarthTripException;
import java.util.Optional;
import java.util.UUID;

public interface CurrentActor {

    Optional<UUID> currentUserId();

    Optional<UUID> currentSessionId();

    default UUID requireUserId() {
        return currentUserId()
                .orElseThrow(
                        () ->
                                EarthTripException.unauthorized(
                                        "AUTHENTICATION_REQUIRED", "로그인이 필요합니다."));
    }

    default UUID requireSessionId() {
        return currentSessionId()
                .orElseThrow(
                        () ->
                                EarthTripException.unauthorized(
                                        "AUTHENTICATION_REQUIRED", "로그인이 필요합니다."));
    }
}
