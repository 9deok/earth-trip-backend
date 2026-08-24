package com.earthtrip.platform.application.port.in;

import java.util.UUID;

/** 실시간 여행 세션을 열거나 사용할 수 있는지 확인하는 입력 포트입니다. */
public interface TripRealtimeSessionUseCase {

    void authorize(UUID tripId, UUID actorUserId);
}
