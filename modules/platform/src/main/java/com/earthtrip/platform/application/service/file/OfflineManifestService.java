package com.earthtrip.platform.application.service.file;

import com.earthtrip.platform.application.port.in.OfflineManifestUseCase;
import com.earthtrip.platform.application.port.out.ContentDigestPort;
import com.earthtrip.platform.application.port.out.FileStorePort;
import com.earthtrip.trip.api.TripAccess;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class OfflineManifestService implements OfflineManifestUseCase {

    private final TripAccess access;
    private final FileStorePort files;
    private final ContentDigestPort contentDigest;
    private final Clock clock;

    OfflineManifestService(
            TripAccess access, FileStorePort files, ContentDigestPort contentDigest, Clock clock) {
        this.access = access;
        this.files = files;
        this.contentDigest = contentDigest;
        this.clock = clock;
    }

    @Override
    public ManifestResult get(UUID tripId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        List<ManifestFile> items =
                files.linksForTrip(tripId).stream()
                        .filter(
                                link ->
                                        !link.visibility().equals("PRIVATE")
                                                || link.linkedBy().equals(actorUserId))
                        .map(link -> manifestFile(link, files.file(link.fileId()).orElse(null)))
                        .filter(java.util.Objects::nonNull)
                        .collect(
                                Collectors.toMap(
                                        ManifestFile::fileId,
                                        Function.identity(),
                                        (first, ignored) -> first,
                                        LinkedHashMap::new))
                        .values()
                        .stream()
                        .sorted(Comparator.comparing(item -> item.fileId().toString()))
                        .toList();
        return new ManifestResult(
                tripId,
                manifestVersion(items),
                clock.instant(),
                items.stream().mapToLong(ManifestFile::sizeBytes).sum(),
                items);
    }

    private static ManifestFile manifestFile(
            FileStorePort.LinkRecord link, FileStorePort.FileRecord file) {
        if (file == null || file.deletedAt() != null || !file.status().equals("READY")) {
            return null;
        }
        return new ManifestFile(
                file.id(),
                file.fileName(),
                file.mimeType(),
                file.sizeBytes(),
                file.checksum(),
                file.version(),
                link.resourceType(),
                link.resourceId());
    }

    private String manifestVersion(List<ManifestFile> files) {
        StringBuilder canonical = new StringBuilder();
        for (ManifestFile file : files) {
            canonical
                    .append(file.fileId())
                    .append(':')
                    .append(file.version())
                    .append(':')
                    .append(file.checksumSha256())
                    .append(':')
                    .append(file.resourceType())
                    .append(':')
                    .append(file.resourceId())
                    .append('\n');
        }
        return contentDigest.sha256(canonical.toString().getBytes(StandardCharsets.UTF_8));
    }
}
