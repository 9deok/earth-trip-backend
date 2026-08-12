package com.earthtrip.planning.application.service.sync;

import com.earthtrip.planning.application.port.in.OfflineOperationUseCase;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class OfflineOperationCodec {

    private OfflineOperationCodec() {}

    static Map<String, Object> command(OfflineOperationUseCase.OperationCommand command) {
        Map<String, Object> value = new LinkedHashMap<>();
        put(value, "operationId", command.operationId());
        put(value, "action", command.action());
        put(value, "resourceType", command.resourceType());
        put(value, "resourceId", command.resourceId());
        put(value, "parentId", command.parentId());
        put(value, "localDate", command.localDate());
        put(value, "payload", command.payload());
        put(value, "status", command.status());
        put(value, "sortOrder", command.sortOrder());
        value.put("baseVersion", command.baseVersion());
        put(value, "stateType", command.stateType());
        put(value, "stateValue", command.stateValue());
        return value;
    }

    @SuppressWarnings("unchecked")
    static OfflineOperationUseCase.OperationCommand command(Map<String, Object> value) {
        return new OfflineOperationUseCase.OperationCommand(
                uuid(value.get("operationId")),
                text(value.get("action")),
                text(value.get("resourceType")),
                uuid(value.get("resourceId")),
                uuid(value.get("parentId")),
                date(value.get("localDate")),
                (Map<String, Object>) value.get("payload"),
                text(value.get("status")),
                integer(value.get("sortOrder")),
                number(value.get("baseVersion")),
                text(value.get("stateType")),
                (Map<String, Object>) value.get("stateValue"));
    }

    static Map<String, Object> resource(PlanningResourceUseCase.ResourceResult resource) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("resourceId", resource.resourceId().toString());
        value.put("resourceType", resource.resourceType());
        put(value, "parentId", resource.parentId());
        put(value, "localDate", resource.localDate());
        value.put("payload", resource.payload());
        value.put("status", resource.status());
        value.put("sortOrder", resource.sortOrder());
        value.put("version", resource.version());
        value.put("updatedAt", resource.updatedAt().toString());
        return value;
    }

    static UUID uuid(Object value) {
        return value == null ? null : UUID.fromString(String.valueOf(value));
    }

    private static LocalDate date(Object value) {
        return value == null ? null : LocalDate.parse(String.valueOf(value));
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer integer(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private static long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value.toString());
            if (value instanceof Map<?, ?>) {
                target.put(key, value);
            }
        }
    }
}
