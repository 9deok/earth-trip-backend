package com.earthtrip.identity.adapter.out.serialization;

import com.earthtrip.identity.application.port.out.SupportDiagnosticsSerializationPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class JacksonSupportDiagnosticsSerializationAdapter implements SupportDiagnosticsSerializationPort {

    private final ObjectMapper objectMapper;

    JacksonSupportDiagnosticsSerializationAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(Map<String, Object> diagnostics) {
        try {
            return objectMapper.writeValueAsString(diagnostics);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("진단정보를 JSON으로 변환할 수 없습니다.", exception);
        }
    }
}
