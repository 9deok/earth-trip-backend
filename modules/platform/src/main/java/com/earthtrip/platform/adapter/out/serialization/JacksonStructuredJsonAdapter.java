package com.earthtrip.platform.adapter.out.serialization;

import com.earthtrip.platform.application.port.out.StructuredJsonPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class JacksonStructuredJsonAdapter implements StructuredJsonPort {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    JacksonStructuredJsonAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> deserializeObject(String value) {
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("JSON 객체를 읽을 수 없습니다.", exception);
        }
    }

    @Override
    public byte[] serializePretty(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("JSON을 생성할 수 없습니다.", exception);
        }
    }
}
