package com.earthtrip.platform.application.port.out;

import java.util.Map;

public interface StructuredJsonPort {
    Map<String, Object> deserializeObject(String value);

    byte[] serializePretty(Object value);
}
