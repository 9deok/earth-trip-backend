package com.earthtrip.trip.spi;

import java.util.Map;
import java.util.UUID;

/** 다른 모듈이 여행 작업공간 변경을 동기화 피드에 기록하도록 제공하는 SPI입니다. */
public interface TripChangePublisher {

    void publish(
            UUID tripId,
            UUID actorUserId,
            String action,
            String resourceType,
            UUID resourceId,
            Map<String, Object> details);

    default void publish(
            UUID tripId, UUID actorUserId, String action, String resourceType, UUID resourceId) {
        publish(tripId, actorUserId, action, resourceType, resourceId, Map.of());
    }
}
