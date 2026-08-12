package com.earthtrip.platform.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FileUseCase {

    UploadResult createUpload(
            UUID userId,
            UUID requestId,
            String fileName,
            String mimeType,
            long sizeBytes,
            String checksum);

    UploadResult uploadStatus(UUID userId, UUID uploadSessionId);

    void abortUpload(UUID userId, UUID uploadSessionId);

    FileResult complete(UUID userId, UUID fileId, UUID uploadSessionId);

    FileResult get(UUID userId, UUID fileId);

    void delete(UUID userId, UUID fileId, long baseVersion);

    DownloadResult download(UUID userId, UUID fileId);

    List<LinkResult> links(UUID userId, UUID fileId);

    List<FileResult> linkedFiles(UUID userId, UUID tripId, String resourceType, UUID resourceId);

    LinkResult link(
            UUID userId,
            UUID fileId,
            UUID requestId,
            UUID tripId,
            String resourceType,
            UUID resourceId,
            String visibility);

    void unlink(UUID userId, UUID fileId, UUID linkId);

    void unlinkResourceFile(
            UUID userId, UUID tripId, String resourceType, UUID resourceId, UUID fileId);

    record UploadResult(
            UUID uploadSessionId,
            UUID fileId,
            String status,
            String uploadUrl,
            Instant expiresAt) {}

    record FileResult(
            UUID fileId,
            String fileName,
            String mimeType,
            long sizeBytes,
            String checksumSha256,
            String status,
            long version,
            Instant createdAt,
            Instant completedAt) {}

    record DownloadResult(UUID fileId, String downloadUrl, Instant expiresAt) {}

    record LinkResult(
            UUID linkId,
            UUID fileId,
            UUID tripId,
            String resourceType,
            UUID resourceId,
            String visibility,
            UUID linkedBy,
            Instant linkedAt) {}
}
