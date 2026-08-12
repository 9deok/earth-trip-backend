package com.earthtrip.planning.application.service.execution;

import java.util.Map;
import java.util.UUID;

record RawDiagnostic(
        String code, String severity, UUID itemId, String message, Map<String, Object> details) {}
