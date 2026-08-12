package com.earthtrip.planning.adapter.out.serialization;

import com.earthtrip.planning.application.port.out.ScheduleSnapshotSerializationPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class JacksonScheduleSnapshotSerializationAdapter implements ScheduleSnapshotSerializationPort {

    private static final TypeReference<List<SnapshotItem>> SNAPSHOT_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    JacksonScheduleSnapshotSerializationAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(List<SnapshotItem> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("일정 변경 스냅샷을 저장할 수 없습니다.", exception);
        }
    }

    @Override
    public List<SnapshotItem> deserialize(String value) {
        try {
            return objectMapper.readValue(value, SNAPSHOT_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 일정 변경 스냅샷을 읽을 수 없습니다.", exception);
        }
    }
}
