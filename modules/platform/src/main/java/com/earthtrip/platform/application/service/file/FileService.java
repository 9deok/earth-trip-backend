package com.earthtrip.platform.application.service.file;

import com.earthtrip.platform.application.port.in.FileUseCase;
import com.earthtrip.platform.application.port.out.FileStorePort;
import com.earthtrip.platform.application.port.out.MalwareScannerPort;
import com.earthtrip.platform.application.port.out.ObjectStoragePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class FileService implements FileUseCase {

    private final FileStorePort store;
    private final ObjectStoragePort objectStorage;
    private final MalwareScannerPort malwareScanner;
    private final TripAccess tripAccess;
    private final Clock clock;

    FileService(
            FileStorePort store,
            ObjectStoragePort objectStorage,
            MalwareScannerPort malwareScanner,
            TripAccess tripAccess,
            Clock clock) {
        this.store = store;
        this.objectStorage = objectStorage;
        this.malwareScanner = malwareScanner;
        this.tripAccess = tripAccess;
        this.clock = clock;
    }

    @Override
    public UploadResult createUpload(
            UUID userId,
            UUID requestId,
            String fileName,
            String mimeType,
            long sizeBytes,
            String checksum) {
        Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");
        Objects.requireNonNull(requestId, "요청 ID는 필수입니다.");
        String safeName = FilePolicy.fileName(fileName);
        String safeMime = FilePolicy.mimeType(mimeType);
        FilePolicy.size(sizeBytes);
        String safeChecksum = FilePolicy.checksum(checksum);

        FileStorePort.FileRecord existing = store.file(requestId).orElse(null);
        if (existing != null) {
            requireUploadRetry(existing, userId, safeName, safeMime, sizeBytes, safeChecksum);
        }

        String storageKey =
                existing == null
                        ? "users/" + userId + "/files/" + requestId
                        : existing.storageKey();
        ObjectStoragePort.SignedUrl signed =
                objectStorage.upload(storageKey, safeMime, sizeBytes, safeChecksum);
        Instant now = clock.instant();
        FileStorePort.FileRecord file =
                existing == null
                        ? new FileStorePort.FileRecord(
                                requestId,
                                userId,
                                safeName,
                                safeMime,
                                sizeBytes,
                                safeChecksum,
                                storageKey,
                                "PENDING_UPLOAD",
                                now,
                                null,
                                null,
                                0)
                        : existing;
        store.saveFile(file);

        UUID uploadSessionId = UUID.randomUUID();
        store.saveUpload(
                new FileStorePort.UploadRecord(
                        uploadSessionId, file.id(), "PENDING", signed.expiresAt(), now, null));
        return new UploadResult(
                uploadSessionId, file.id(), "PENDING", signed.url(), signed.expiresAt());
    }

    @Override
    public UploadResult uploadStatus(UUID userId, UUID uploadSessionId) {
        FileStorePort.UploadRecord upload = loadUpload(uploadSessionId);
        FileStorePort.FileRecord file = requireOwner(loadFile(upload.fileId()), userId);
        if (upload.status().equals("PENDING") && !upload.expiresAt().isAfter(clock.instant())) {
            upload =
                    store.saveUpload(
                            new FileStorePort.UploadRecord(
                                    upload.id(),
                                    upload.fileId(),
                                    "EXPIRED",
                                    upload.expiresAt(),
                                    upload.createdAt(),
                                    upload.abortedAt()));
        }
        return new UploadResult(upload.id(), file.id(), upload.status(), null, upload.expiresAt());
    }

    @Override
    public void abortUpload(UUID userId, UUID uploadSessionId) {
        FileStorePort.UploadRecord upload = loadUpload(uploadSessionId);
        requireOwner(loadFile(upload.fileId()), userId);
        if (upload.status().equals("COMPLETED")) {
            throw EarthTripException.conflict(
                    "UPLOAD_ALREADY_COMPLETED", "이미 완료된 업로드는 중단할 수 없습니다.");
        }
        if (upload.status().equals("ABORTED")) {
            return;
        }
        Instant now = clock.instant();
        store.saveUpload(
                new FileStorePort.UploadRecord(
                        upload.id(),
                        upload.fileId(),
                        "ABORTED",
                        upload.expiresAt(),
                        upload.createdAt(),
                        now));
    }

    @Override
    public FileResult complete(UUID userId, UUID fileId, UUID uploadSessionId) {
        FileStorePort.FileRecord file = requireOwner(loadFile(fileId), userId);
        FileStorePort.UploadRecord upload = loadUpload(uploadSessionId);
        if (!upload.fileId().equals(file.id())) {
            throw EarthTripException.badRequest("UPLOAD_FILE_MISMATCH", "업로드 세션과 파일이 일치하지 않습니다.");
        }
        if (file.status().equals("READY") || file.status().equals("SCANNING")) {
            return result(file);
        }
        if (!upload.status().equals("PENDING")) {
            throw EarthTripException.conflict("UPLOAD_NOT_PENDING", "완료할 수 있는 업로드 상태가 아닙니다.");
        }
        Instant now = clock.instant();
        if (!upload.expiresAt().isAfter(now)) {
            store.saveUpload(
                    new FileStorePort.UploadRecord(
                            upload.id(),
                            upload.fileId(),
                            "EXPIRED",
                            upload.expiresAt(),
                            upload.createdAt(),
                            upload.abortedAt()));
            throw EarthTripException.conflict(
                    "UPLOAD_SESSION_EXPIRED", "업로드 세션이 만료되었습니다. 새 업로드 세션을 발급해 주세요.");
        }

        objectStorage.verifyUpload(
                file.storageKey(), file.mimeType(), file.sizeBytes(), file.checksum());
        FileStorePort.FileRecord scanning =
                store.saveFile(
                        new FileStorePort.FileRecord(
                                file.id(),
                                file.ownerUserId(),
                                file.fileName(),
                                file.mimeType(),
                                file.sizeBytes(),
                                file.checksum(),
                                file.storageKey(),
                                "SCANNING",
                                file.createdAt(),
                                now,
                                null,
                                file.version()));
        MalwareScannerPort.ScanResult scan = malwareScanner.scan(file.storageKey());
        String finalStatus = scan.clean() ? "READY" : "QUARANTINED";
        FileStorePort.FileRecord completed =
                store.saveFile(
                        new FileStorePort.FileRecord(
                                scanning.id(),
                                scanning.ownerUserId(),
                                scanning.fileName(),
                                scanning.mimeType(),
                                scanning.sizeBytes(),
                                scanning.checksum(),
                                scanning.storageKey(),
                                finalStatus,
                                scanning.createdAt(),
                                now,
                                null,
                                scanning.version()));
        store.saveUpload(
                new FileStorePort.UploadRecord(
                        upload.id(),
                        upload.fileId(),
                        "COMPLETED",
                        upload.expiresAt(),
                        upload.createdAt(),
                        upload.abortedAt()));
        return result(completed);
    }

    @Override
    @Transactional(readOnly = true)
    public FileResult get(UUID userId, UUID fileId) {
        FileStorePort.FileRecord file = loadActiveFile(fileId);
        requireFileAccess(file, userId);
        return result(file);
    }

    @Override
    public void delete(UUID userId, UUID fileId, long baseVersion) {
        FileStorePort.FileRecord file = requireOwner(loadActiveFile(fileId), userId);
        requireVersion(file, baseVersion);
        if (!store.links(fileId).isEmpty()) {
            throw EarthTripException.conflict(
                    "FILE_STILL_LINKED", "연결된 리소스가 있어 파일을 삭제할 수 없습니다. 먼저 연결을 해제해 주세요.");
        }
        store.saveFile(
                new FileStorePort.FileRecord(
                        file.id(),
                        file.ownerUserId(),
                        file.fileName(),
                        file.mimeType(),
                        file.sizeBytes(),
                        file.checksum(),
                        file.storageKey(),
                        "DELETED",
                        file.createdAt(),
                        file.completedAt(),
                        clock.instant(),
                        file.version()));
        objectStorage.delete(file.storageKey());
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadResult download(UUID userId, UUID fileId) {
        FileStorePort.FileRecord file = loadActiveFile(fileId);
        requireFileAccess(file, userId);
        if (!file.status().equals("READY")) {
            throw EarthTripException.conflict("FILE_NOT_READY", "업로드가 완료된 파일만 다운로드할 수 있습니다.");
        }
        ObjectStoragePort.SignedUrl signed = objectStorage.download(file.storageKey());
        return new DownloadResult(file.id(), signed.url(), signed.expiresAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkResult> links(UUID userId, UUID fileId) {
        FileStorePort.FileRecord file = loadActiveFile(fileId);
        requireFileAccess(file, userId);
        return store.links(fileId).stream().map(FileService::result).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileResult> linkedFiles(
            UUID userId, UUID tripId, String resourceType, UUID resourceId) {
        tripAccess.requireViewer(tripId, userId);
        String type = FilePolicy.resourceType(resourceType);
        return store.links(tripId, type, resourceId).stream()
                .filter(link -> visible(link, userId))
                .map(link -> loadActiveFile(link.fileId()))
                .filter(file -> file.status().equals("READY"))
                .map(FileService::result)
                .toList();
    }

    @Override
    public LinkResult link(
            UUID userId,
            UUID fileId,
            UUID requestId,
            UUID tripId,
            String resourceType,
            UUID resourceId,
            String visibility) {
        tripAccess.requireEditor(tripId, userId);
        FileStorePort.FileRecord file = requireOwner(loadActiveFile(fileId), userId);
        if (!file.status().equals("READY")) {
            throw EarthTripException.conflict("FILE_NOT_READY", "업로드가 완료된 파일만 연결할 수 있습니다.");
        }
        String type = FilePolicy.resourceType(resourceType);
        String safeVisibility = FilePolicy.visibility(visibility);
        FileStorePort.LinkRecord sameTarget =
                store.links(fileId).stream()
                        .filter(link -> link.tripId().equals(tripId))
                        .filter(link -> link.resourceType().equals(type))
                        .filter(link -> link.resourceId().equals(resourceId))
                        .findFirst()
                        .orElse(null);
        if (sameTarget != null) {
            return result(sameTarget);
        }
        FileStorePort.LinkRecord existing = store.link(requestId).orElse(null);
        if (existing != null) {
            if (!sameLink(existing, fileId, tripId, type, resourceId, safeVisibility, userId)) {
                throw EarthTripException.conflict(
                        "IDEMPOTENCY_KEY_REUSED", "이미 다른 파일 연결에 사용된 요청 ID입니다.");
            }
            return result(existing);
        }
        return result(
                store.saveLink(
                        new FileStorePort.LinkRecord(
                                requestId,
                                fileId,
                                tripId,
                                type,
                                resourceId,
                                safeVisibility,
                                userId,
                                clock.instant())));
    }

    @Override
    public void unlink(UUID userId, UUID fileId, UUID linkId) {
        FileStorePort.FileRecord file = loadActiveFile(fileId);
        FileStorePort.LinkRecord link = loadLink(linkId);
        if (!link.fileId().equals(file.id())) {
            throw linkNotFound();
        }
        tripAccess.requireEditor(link.tripId(), userId);
        store.deleteLink(link.id());
    }

    @Override
    public void unlinkResourceFile(
            UUID userId, UUID tripId, String resourceType, UUID resourceId, UUID fileId) {
        tripAccess.requireEditor(tripId, userId);
        String type = FilePolicy.resourceType(resourceType);
        FileStorePort.LinkRecord link =
                store.links(tripId, type, resourceId).stream()
                        .filter(candidate -> candidate.fileId().equals(fileId))
                        .findFirst()
                        .orElseThrow(FileService::linkNotFound);
        store.deleteLink(link.id());
    }

    private void requireFileAccess(FileStorePort.FileRecord file, UUID userId) {
        if (file.ownerUserId().equals(userId)) {
            return;
        }
        for (FileStorePort.LinkRecord link : store.links(file.id())) {
            if (!visible(link, userId)) {
                continue;
            }
            try {
                tripAccess.requireViewer(link.tripId(), userId);
                return;
            } catch (EarthTripException denied) {
                if (denied.httpStatus() != 403 && denied.httpStatus() != 404) {
                    throw denied;
                }
            }
        }
        throw fileNotFound();
    }

    private FileStorePort.FileRecord loadFile(UUID fileId) {
        return store.file(fileId).orElseThrow(FileService::fileNotFound);
    }

    private FileStorePort.FileRecord loadActiveFile(UUID fileId) {
        FileStorePort.FileRecord file = loadFile(fileId);
        if (file.deletedAt() != null || file.status().equals("DELETED")) {
            throw fileNotFound();
        }
        return file;
    }

    private FileStorePort.UploadRecord loadUpload(UUID uploadSessionId) {
        return store.upload(uploadSessionId)
                .orElseThrow(
                        () ->
                                EarthTripException.notFound(
                                        "UPLOAD_SESSION_NOT_FOUND", "업로드 세션을 찾을 수 없습니다."));
    }

    private FileStorePort.LinkRecord loadLink(UUID linkId) {
        return store.link(linkId).orElseThrow(FileService::linkNotFound);
    }

    private static FileStorePort.FileRecord requireOwner(
            FileStorePort.FileRecord file, UUID userId) {
        if (!file.ownerUserId().equals(userId)) {
            throw fileNotFound();
        }
        return file;
    }

    private static void requireUploadRetry(
            FileStorePort.FileRecord existing,
            UUID userId,
            String fileName,
            String mimeType,
            long sizeBytes,
            String checksum) {
        boolean same =
                existing.ownerUserId().equals(userId)
                        && existing.fileName().equals(fileName)
                        && existing.mimeType().equals(mimeType)
                        && existing.sizeBytes() == sizeBytes
                        && existing.checksum().equals(checksum)
                        && existing.status().equals("PENDING_UPLOAD")
                        && existing.deletedAt() == null;
        if (!same) {
            throw EarthTripException.conflict(
                    "IDEMPOTENCY_KEY_REUSED", "이미 다른 파일 업로드에 사용된 요청 ID입니다.");
        }
    }

    private static boolean visible(FileStorePort.LinkRecord link, UUID userId) {
        return link.visibility().equals("TRIP") || link.linkedBy().equals(userId);
    }

    private static boolean sameLink(
            FileStorePort.LinkRecord link,
            UUID fileId,
            UUID tripId,
            String resourceType,
            UUID resourceId,
            String visibility,
            UUID userId) {
        return link.fileId().equals(fileId)
                && link.tripId().equals(tripId)
                && link.resourceType().equals(resourceType)
                && link.resourceId().equals(resourceId)
                && link.visibility().equals(visibility)
                && link.linkedBy().equals(userId);
    }

    private static void requireVersion(FileStorePort.FileRecord file, long baseVersion) {
        if (file.version() != baseVersion) {
            throw new EarthTripException(
                    "VERSION_CONFLICT",
                    409,
                    "다른 파일 변경이 먼저 저장되었습니다.",
                    Map.of("serverVersion", file.version()));
        }
    }

    private static FileResult result(FileStorePort.FileRecord file) {
        return new FileResult(
                file.id(),
                file.fileName(),
                file.mimeType(),
                file.sizeBytes(),
                file.checksum(),
                file.status(),
                file.version(),
                file.createdAt(),
                file.completedAt());
    }

    private static LinkResult result(FileStorePort.LinkRecord link) {
        return new LinkResult(
                link.id(),
                link.fileId(),
                link.tripId(),
                link.resourceType(),
                link.resourceId(),
                link.visibility(),
                link.linkedBy(),
                link.linkedAt());
    }

    private static EarthTripException fileNotFound() {
        return EarthTripException.notFound("FILE_NOT_FOUND", "파일을 찾을 수 없습니다.");
    }

    private static EarthTripException linkNotFound() {
        return EarthTripException.notFound("FILE_LINK_NOT_FOUND", "파일 연결을 찾을 수 없습니다.");
    }
}
