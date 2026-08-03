package com.earthtrip.platform.adapter.in.realtime;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
class TripRealtimeWebSocketHandler extends TextWebSocketHandler {

    private static final int MAX_MESSAGE_BYTES = 16 * 1024;
    private static final int SEND_TIME_LIMIT_MILLIS = 5_000;
    private static final int SEND_BUFFER_BYTES = 64 * 1024;
    private static final Set<String> CLIENT_MESSAGE_TYPES = Set.of("PING", "PRESENCE", "EDITING");
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() { };

    private final TripAccess tripAccess;
    private final ObjectMapper json;
    private final Clock clock;
    private final Map<UUID, Map<String, SessionMember>> rooms = new ConcurrentHashMap<>();

    TripRealtimeWebSocketHandler(TripAccess tripAccess, ObjectMapper json, Clock clock) {
        this.tripAccess = tripAccess;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession rawSession) throws Exception {
        UUID tripId = tripId(rawSession);
        UUID userId = userId(rawSession);
        tripAccess.requireViewer(tripId, userId);
        WebSocketSession session = new ConcurrentWebSocketSessionDecorator(
            rawSession,
            SEND_TIME_LIMIT_MILLIS,
            SEND_BUFFER_BYTES
        );
        rooms.computeIfAbsent(tripId, ignored -> new ConcurrentHashMap<>())
            .put(rawSession.getId(), new SessionMember(session, userId));
        send(session, Map.of(
            "type", "CONNECTED",
            "tripId", tripId.toString(),
            "userId", userId.toString(),
            "onlineCount", rooms.get(tripId).size(),
            "occurredAt", clock.instant().toString()
        ));
        broadcast(tripId, Map.of(
            "type", "PRESENCE",
            "userId", userId.toString(),
            "status", "ONLINE",
            "onlineCount", rooms.get(tripId).size(),
            "occurredAt", clock.instant().toString()
        ), rawSession.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
        throws Exception {
        if (message.getPayloadLength() > MAX_MESSAGE_BYTES) {
            session.close(CloseStatus.TOO_BIG_TO_PROCESS);
            return;
        }
        UUID tripId = tripId(session);
        UUID userId = userId(session);
        tripAccess.requireViewer(tripId, userId);
        Map<String, Object> payload = read(message.getPayload());
        String type = text(payload, "type").toUpperCase(java.util.Locale.ROOT);
        if (!CLIENT_MESSAGE_TYPES.contains(type)) {
            sendError(session, "UNSUPPORTED_REALTIME_MESSAGE", "지원하지 않는 실시간 메시지입니다.");
            return;
        }
        if (type.equals("PING")) {
            send(session, Map.of("type", "PONG", "occurredAt", clock.instant().toString()));
            return;
        }
        if (type.equals("PRESENCE")) {
            String status = text(payload, "status").toUpperCase(java.util.Locale.ROOT);
            if (!Set.of("ACTIVE", "AWAY").contains(status)) {
                sendError(session, "INVALID_PRESENCE_STATUS", "presence 상태를 확인해 주세요.");
                return;
            }
            broadcast(tripId, Map.of(
                "type", "PRESENCE",
                "userId", userId.toString(),
                "status", status,
                "onlineCount", rooms.getOrDefault(tripId, Map.of()).size(),
                "occurredAt", clock.instant().toString()
            ), null);
            return;
        }
        String resourceType = text(payload, "resourceType");
        String resourceId = text(payload, "resourceId");
        if (resourceType.length() > 50 || resourceId.length() > 160) {
            sendError(session, "INVALID_EDITING_TARGET", "편집 대상 정보가 너무 깁니다.");
            return;
        }
        Object activeValue = payload.get("active");
        if (!(activeValue instanceof Boolean active)) {
            sendError(session, "INVALID_EDITING_STATE", "editing.active는 boolean이어야 합니다.");
            return;
        }
        broadcast(tripId, Map.of(
            "type", "EDITING",
            "userId", userId.toString(),
            "resourceType", resourceType,
            "resourceId", resourceId,
            "active", active,
            "occurredAt", clock.instant().toString()
        ), null);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception)
        throws Exception {
        remove(session, "TRANSPORT_ERROR");
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        remove(session, "OFFLINE");
    }

    private void remove(WebSocketSession session, String status) {
        UUID tripId;
        try {
            tripId = tripId(session);
        } catch (RuntimeException exception) {
            return;
        }
        Map<String, SessionMember> room = rooms.get(tripId);
        if (room == null) {
            return;
        }
        SessionMember removed = room.remove(session.getId());
        if (room.isEmpty()) {
            rooms.remove(tripId, room);
        }
        if (removed != null) {
            broadcast(tripId, Map.of(
                "type", "PRESENCE",
                "userId", removed.userId().toString(),
                "status", status,
                "onlineCount", room.size(),
                "occurredAt", clock.instant().toString()
            ), null);
        }
    }

    private void broadcast(UUID tripId, Map<String, Object> payload, String excludedSessionId) {
        Map<String, SessionMember> room = rooms.get(tripId);
        if (room == null) {
            return;
        }
        for (var entry : new ArrayList<>(room.entrySet())) {
            if (entry.getKey().equals(excludedSessionId)) {
                continue;
            }
            try {
                send(entry.getValue().session(), payload);
            } catch (IOException exception) {
                room.remove(entry.getKey());
                try {
                    entry.getValue().session().close(CloseStatus.SERVER_ERROR);
                } catch (IOException ignored) {
                    // 전송 실패 후 닫기 실패는 다음 연결 정리에서 제거됩니다.
                }
            }
        }
    }

    private void send(WebSocketSession session, Map<String, Object> payload) throws IOException {
        try {
            session.sendMessage(new TextMessage(json.writeValueAsString(payload)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("실시간 메시지를 JSON으로 만들 수 없습니다.", exception);
        }
    }

    private void sendError(WebSocketSession session, String code, String detail)
        throws IOException {
        send(session, Map.of(
            "type", "ERROR",
            "code", code,
            "detail", detail,
            "occurredAt", clock.instant().toString()
        ));
    }

    private Map<String, Object> read(String payload) {
        try {
            Map<String, Object> result = json.readValue(payload, MAP);
            return result == null ? Map.of() : new LinkedHashMap<>(result);
        } catch (JsonProcessingException exception) {
            throw EarthTripException.badRequest(
                "INVALID_REALTIME_MESSAGE",
                "실시간 메시지는 JSON 객체여야 합니다."
            );
        }
    }

    private static UUID tripId(WebSocketSession session) {
        String path = session.getUri() == null ? "" : session.getUri().getPath();
        String prefix = "/ws/v1/trips/";
        if (!path.startsWith(prefix)) {
            throw EarthTripException.badRequest(
                "INVALID_REALTIME_TRIP_PATH",
                "실시간 여행 경로가 올바르지 않습니다."
            );
        }
        try {
            return UUID.fromString(path.substring(prefix.length()));
        } catch (IllegalArgumentException exception) {
            throw EarthTripException.badRequest(
                "INVALID_REALTIME_TRIP_ID",
                "실시간 여행 ID가 올바르지 않습니다."
            );
        }
    }

    private static UUID userId(WebSocketSession session) {
        if (session.getPrincipal() == null) {
            throw EarthTripException.unauthorized(
                "REALTIME_AUTHENTICATION_REQUIRED",
                "실시간 연결에는 로그인이 필요합니다."
            );
        }
        try {
            return UUID.fromString(session.getPrincipal().getName());
        } catch (IllegalArgumentException exception) {
            throw EarthTripException.unauthorized(
                "INVALID_REALTIME_PRINCIPAL",
                "실시간 연결의 사용자 정보가 올바르지 않습니다."
            );
        }
    }

    private static String text(Map<String, Object> payload, String field) {
        Object value = payload.get(field);
        if (value == null || value.toString().isBlank()) {
            throw EarthTripException.badRequest(
                "REALTIME_FIELD_REQUIRED",
                "실시간 메시지에 " + field + " 값이 필요합니다."
            );
        }
        return value.toString().strip();
    }

    private record SessionMember(WebSocketSession session, UUID userId) { }
}
