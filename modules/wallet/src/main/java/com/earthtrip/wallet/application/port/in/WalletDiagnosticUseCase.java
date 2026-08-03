package com.earthtrip.wallet.application.port.in;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface WalletDiagnosticUseCase {

    List<DiagnosticResult> list(UUID tripId, UUID actorUserId);

    record DiagnosticResult(
        UUID diagnosticId,
        String code,
        String severity,
        UUID recordId,
        String message,
        Map<String, Object> details
    ) { }
}
