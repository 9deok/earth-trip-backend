package com.earthtrip.platform.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileStorePort {

    Optional<FileRecord> file(UUID id);

    FileRecord saveFile(FileRecord record);

    Optional<UploadRecord> upload(UUID id);

    UploadRecord saveUpload(UploadRecord record);

    List<LinkRecord> links(UUID fileId);

    List<LinkRecord> linksForTrip(UUID tripId);

    List<LinkRecord> links(UUID tripId, String resourceType, UUID resourceId);

    Optional<LinkRecord> link(UUID id);

    LinkRecord saveLink(LinkRecord record);

    void deleteLink(UUID id);

    record FileRecord(
        UUID id,
        UUID ownerUserId,
        String fileName,
        String mimeType,
        long sizeBytes,
        String checksum,
        String storageKey,
        String status,
        Instant createdAt,
        Instant completedAt,
        Instant deletedAt,
        long version
    ) { }

    record UploadRecord(
        UUID id,
        UUID fileId,
        String status,
        Instant expiresAt,
        Instant createdAt,
        Instant abortedAt
    ) { }

    record LinkRecord(
        UUID id,
        UUID fileId,
        UUID tripId,
        String resourceType,
        UUID resourceId,
        String visibility,
        UUID linkedBy,
        Instant linkedAt
    ) { }
}
