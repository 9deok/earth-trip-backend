package com.earthtrip.trip.api;

import java.util.UUID;

/** 커밋된 여행 변경을 현재 접속 중인 클라이언트에 알리는 공개 계약입니다. */
public interface TripRealtimeNotifier {

    void notifyChange(UUID tripId, String action, String resourceType, UUID resourceId);
}
