package com.earthtrip.identity.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface PersonalDataExporterPort {

    ExportArtifact export(UUID userId, UUID exportId, String format);

    record ExportArtifact(UUID fileId, Instant expiresAt) { }
}
