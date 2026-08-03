package com.earthtrip.platform.application.service.export;

import com.earthtrip.platform.application.port.in.TripExportUseCase;
import com.earthtrip.platform.application.port.out.TripExportStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class TripExportService implements TripExportUseCase {

    private static final Set<String> FORMATS = Set.of("PDF", "ICS", "KML", "CSV", "JSON");
    private static final Set<String> SCOPES = Set.of(
        "TRIP", "STRUCTURE", "PLANNING", "WALLET", "EXPENSE"
    );

    private final TripAccess access;
    private final TripExportStorePort store;
    private final TripExportRenderer renderer;
    private final Clock clock;

    TripExportService(
        TripAccess access,
        TripExportStorePort store,
        TripExportRenderer renderer,
        Clock clock
    ) {
        this.access = access;
        this.store = store;
        this.renderer = renderer;
        this.clock = clock;
    }

    @Override
    public ExportResult create(UUID tripId, UUID actorUserId, ExportCommand command) {
        access.requireViewer(tripId, actorUserId);
        if (command == null || command.requestId() == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "requestId가 필요합니다.");
        }
        TripExportStorePort.ExportRecord existing = store.find(command.requestId()).orElse(null);
        if (existing != null) {
            requireScope(existing, tripId, actorUserId);
            return result(existing);
        }
        String format = format(command.format());
        Set<String> scopes = scopes(command.scopes());
        Instant now = clock.instant();
        TripExportStorePort.ExportRecord processing = store.save(
            new TripExportStorePort.ExportRecord(
                command.requestId(), tripId, format, scopes, "PROCESSING", null, null,
                null, null, null, null, 1, actorUserId, now, now, 0
            )
        );
        return result(render(processing, actorUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResult get(UUID tripId, UUID exportId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        return result(loadOwned(tripId, exportId, actorUserId));
    }

    @Override
    public ExportResult retry(
        UUID tripId,
        UUID exportId,
        UUID actorUserId,
        long baseVersion
    ) {
        access.requireViewer(tripId, actorUserId);
        TripExportStorePort.ExportRecord current = loadOwned(tripId, exportId, actorUserId);
        requireVersion(current.version(), baseVersion);
        if (!current.status().equals("FAILED")) {
            throw EarthTripException.conflict(
                "TRIP_EXPORT_NOT_RETRYABLE", "실패한 내보내기만 재시도할 수 있습니다."
            );
        }
        TripExportStorePort.ExportRecord processing = store.save(copy(
            current, "PROCESSING", null, null, null, null, null,
            current.attemptCount() + 1
        ));
        return result(render(processing, actorUserId));
    }

    @Override
    public ExportResult cancel(
        UUID tripId,
        UUID exportId,
        UUID actorUserId,
        long baseVersion
    ) {
        access.requireViewer(tripId, actorUserId);
        TripExportStorePort.ExportRecord current = loadOwned(tripId, exportId, actorUserId);
        requireVersion(current.version(), baseVersion);
        if (current.status().equals("CANCELLED")) {
            return result(current);
        }
        if (current.status().equals("COMPLETED")) {
            throw EarthTripException.conflict(
                "TRIP_EXPORT_NOT_CANCELLABLE", "완료된 내보내기는 취소할 수 없습니다."
            );
        }
        return result(store.save(copy(
            current, "CANCELLED", null, null, null, null, null, current.attemptCount()
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public ArtifactResult artifact(UUID tripId, UUID exportId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        TripExportStorePort.ExportRecord export = loadOwned(tripId, exportId, actorUserId);
        if (!export.status().equals("COMPLETED") || export.artifact() == null) {
            throw EarthTripException.conflict(
                "TRIP_EXPORT_NOT_READY", "내보내기 파일이 아직 준비되지 않았습니다."
            );
        }
        return new ArtifactResult(
            export.fileName(), export.mimeType(), export.artifact().clone()
        );
    }

    private TripExportStorePort.ExportRecord render(
        TripExportStorePort.ExportRecord current,
        UUID actorUserId
    ) {
        try {
            TripExportRenderer.RenderedArtifact artifact = renderer.render(
                current.tripId(), actorUserId, current.format(), current.scopes()
            );
            return store.save(copy(
                current, "COMPLETED", artifact.fileName(), artifact.mimeType(),
                artifact.content(), checksum(artifact.content()), null, current.attemptCount()
            ));
        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null
                ? "내보내기 파일 생성에 실패했습니다."
                : exception.getMessage();
            return store.save(copy(
                current, "FAILED", null, null, null, null,
                message.length() > 500 ? message.substring(0, 500) : message,
                current.attemptCount()
            ));
        }
    }

    private TripExportStorePort.ExportRecord copy(
        TripExportStorePort.ExportRecord current,
        String status,
        String fileName,
        String mimeType,
        byte[] artifact,
        String checksum,
        String failureMessage,
        int attempts
    ) {
        return new TripExportStorePort.ExportRecord(
            current.id(), current.tripId(), current.format(), current.scopes(), status,
            fileName, mimeType, artifact, checksum,
            failureMessage == null ? null : "TRIP_EXPORT_RENDER_FAILED", failureMessage,
            attempts, current.createdBy(), current.createdAt(), clock.instant(), current.version()
        );
    }

    private TripExportStorePort.ExportRecord loadOwned(
        UUID tripId,
        UUID exportId,
        UUID actorUserId
    ) {
        TripExportStorePort.ExportRecord export = store.find(exportId)
            .orElseThrow(() -> EarthTripException.notFound(
                "TRIP_EXPORT_NOT_FOUND", "여행 내보내기 작업을 찾을 수 없습니다."
            ));
        requireScope(export, tripId, actorUserId);
        return export;
    }

    private static void requireScope(
        TripExportStorePort.ExportRecord export,
        UUID tripId,
        UUID actorUserId
    ) {
        if (!export.tripId().equals(tripId) || !export.createdBy().equals(actorUserId)) {
            throw EarthTripException.notFound(
                "TRIP_EXPORT_NOT_FOUND", "여행 내보내기 작업을 찾을 수 없습니다."
            );
        }
    }

    private static String format(String value) {
        String normalized = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        if (!FORMATS.contains(normalized)) {
            throw EarthTripException.badRequest(
                "INVALID_TRIP_EXPORT_FORMAT", "PDF, ICS, KML, CSV, JSON만 지원합니다."
            );
        }
        return normalized;
    }

    private static Set<String> scopes(Set<String> value) {
        if (value == null || value.isEmpty()) {
            return SCOPES;
        }
        Set<String> normalized = new LinkedHashSet<>();
        value.stream().map(item -> item.strip().toUpperCase(Locale.ROOT))
            .forEach(normalized::add);
        if (!SCOPES.containsAll(normalized)) {
            throw EarthTripException.badRequest(
                "INVALID_TRIP_EXPORT_SCOPE", "지원하지 않는 내보내기 범위입니다."
            );
        }
        return Set.copyOf(normalized);
    }

    private static String checksum(byte[] content) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private static ExportResult result(TripExportStorePort.ExportRecord export) {
        return new ExportResult(
            export.id(), export.tripId(), export.format(), export.scopes(), export.status(),
            export.fileName(), export.mimeType(),
            export.artifact() == null ? null : (long) export.artifact().length,
            export.checksumSha256(), export.status().equals("COMPLETED")
                ? "/api/v1/trips/" + export.tripId() + "/exports/" + export.id() + "/download"
                : null,
            export.failureCode(), export.failureMessage(), export.attemptCount(),
            export.createdAt(), export.updatedAt(), export.version()
        );
    }

    private static void requireVersion(long serverVersion, long baseVersion) {
        if (serverVersion != baseVersion) {
            throw new EarthTripException(
                "VERSION_CONFLICT", 409, "다른 내보내기 변경이 먼저 저장되었습니다.",
                Map.of("serverVersion", serverVersion)
            );
        }
    }
}
