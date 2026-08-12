package com.earthtrip.planning.application.port.in;

import java.time.Instant;
import java.util.UUID;

public interface DayDiagnosticResolutionUseCase {

    ResolutionResult resolve(
            UUID tripId, UUID dayId, UUID diagnosticId, UUID actorUserId, String note);

    void reopen(UUID tripId, UUID dayId, UUID diagnosticId, UUID actorUserId);

    record ResolutionResult(UUID diagnosticId, String note, UUID resolvedBy, Instant resolvedAt) {}
}
