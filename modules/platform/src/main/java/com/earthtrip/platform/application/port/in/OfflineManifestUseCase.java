package com.earthtrip.platform.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OfflineManifestUseCase {

    ManifestResult get(UUID tripId, UUID actorUserId);

    record ManifestResult(
        UUID tripId,
        String manifestVersion,
        Instant generatedAt,
        long totalSizeBytes,
        List<ManifestFile> files
    ) { }

    record ManifestFile(
        UUID fileId,
        String fileName,
        String mimeType,
        long sizeBytes,
        String checksumSha256,
        long version,
        String resourceType,
        UUID resourceId
    ) { }
}
