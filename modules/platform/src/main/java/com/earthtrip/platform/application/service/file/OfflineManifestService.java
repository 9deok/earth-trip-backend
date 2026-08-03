package com.earthtrip.platform.application.service.file;

import com.earthtrip.platform.application.port.in.OfflineManifestUseCase;
import com.earthtrip.platform.application.port.out.FileStorePort;
import com.earthtrip.trip.api.TripAccess;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HexFormat;
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
    private final Clock clock;

    OfflineManifestService(TripAccess access, FileStorePort files, Clock clock) {
        this.access = access;
        this.files = files;
        this.clock = clock;
    }

    @Override
    public ManifestResult get(UUID tripId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        List<ManifestFile> items = files.linksForTrip(tripId).stream()
            .filter(link -> !link.visibility().equals("PRIVATE")
                || link.linkedBy().equals(actorUserId))
            .map(link -> manifestFile(link, files.file(link.fileId()).orElse(null)))
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toMap(
                ManifestFile::fileId,
                Function.identity(),
                (first, ignored) -> first,
                LinkedHashMap::new
            ))
            .values()
            .stream()
            .sorted(Comparator.comparing(item -> item.fileId().toString()))
            .toList();
        return new ManifestResult(
            tripId, manifestVersion(items), clock.instant(),
            items.stream().mapToLong(ManifestFile::sizeBytes).sum(), items
        );
    }

    private static ManifestFile manifestFile(
        FileStorePort.LinkRecord link,
        FileStorePort.FileRecord file
    ) {
        if (file == null || file.deletedAt() != null || !file.status().equals("READY")) {
            return null;
        }
        return new ManifestFile(
            file.id(), file.fileName(), file.mimeType(), file.sizeBytes(), file.checksum(),
            file.version(), link.resourceType(), link.resourceId()
        );
    }

    private static String manifestVersion(List<ManifestFile> files) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (ManifestFile file : files) {
                digest.update((file.fileId() + ":" + file.version() + ":"
                    + file.checksumSha256() + ":" + file.resourceType() + ":"
                    + file.resourceId() + "\n").getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }
}
