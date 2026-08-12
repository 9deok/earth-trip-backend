package com.earthtrip.identity.application.port.out;

import java.util.Map;

public interface SupportDiagnosticsSerializationPort {
    String serialize(Map<String, Object> diagnostics);
}
