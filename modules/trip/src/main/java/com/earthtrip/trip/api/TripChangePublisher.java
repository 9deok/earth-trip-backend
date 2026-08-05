package com.earthtrip.trip.api;

import java.util.Map;
import java.util.UUID;

/** 다른 도메인이 여행 작업공간 변경을 동기화 피드에 기록하는 공개 계약입니다. */
public interface TripChangePublisher {

    void publish(
        UUID tripId,
        UUID actorUserId,
        String action,
        String resourceType,
        UUID resourceId,
        Map<String, Object> details
    );

    default void publish(
        UUID tripId,
        UUID actorUserId,
        String action,
        String resourceType,
        UUID resourceId
    ) {
        publish(tripId, actorUserId, action, resourceType, resourceId, Map.of());
    }
}
