package com.earthtrip.identity.adapter.out.export;

import com.earthtrip.identity.application.port.out.AccountDeletionStorePort;
import com.earthtrip.identity.application.port.out.AccountIdentityStorePort;
import com.earthtrip.identity.application.port.out.AuthSessionStorePort;
import com.earthtrip.identity.application.port.out.PersonalDataExporterPort;
import com.earthtrip.identity.application.port.out.PersonalSupportStorePort;
import com.earthtrip.identity.application.port.out.PolicyStorePort;
import com.earthtrip.identity.application.port.out.PreferenceStorePort;
import com.earthtrip.identity.application.port.out.UserAccountStorePort;
import com.earthtrip.identity.domain.AuthSession;
import com.earthtrip.identity.domain.UserAccount;
import com.earthtrip.identity.domain.UserId;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class LocalPersonalDataExporter implements PersonalDataExporterPort {

    private static final Duration DOWNLOAD_TTL = Duration.ofDays(7);

    private final Path root;
    private final UserAccountStorePort accounts;
    private final AccountIdentityStorePort identities;
    private final AuthSessionStorePort sessions;
    private final PreferenceStorePort preferences;
    private final PolicyStorePort policies;
    private final PersonalSupportStorePort support;
    private final AccountDeletionStorePort deletions;
    private final ObjectMapper json;
    private final Clock clock;

    LocalPersonalDataExporter(
        @Value("${earthtrip.exports.local.root:}") String root,
        UserAccountStorePort accounts,
        AccountIdentityStorePort identities,
        AuthSessionStorePort sessions,
        PreferenceStorePort preferences,
        PolicyStorePort policies,
        PersonalSupportStorePort support,
        AccountDeletionStorePort deletions,
        ObjectMapper json,
        Clock clock
    ) {
        this.root = root == null || root.isBlank()
            ? null
            : Path.of(root).toAbsolutePath().normalize();
        this.accounts = accounts;
        this.identities = identities;
        this.sessions = sessions;
        this.preferences = preferences;
        this.policies = policies;
        this.support = support;
        this.deletions = deletions;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public ExportArtifact export(UUID userId, UUID exportId, String format) {
        requireConfigured();
        UserAccount account = accounts.findById(new UserId(userId)).orElseThrow(() ->
            EarthTripException.notFound("ACCOUNT_NOT_FOUND", "계정을 찾을 수 없습니다.")
        );
        byte[] content = serialize(snapshot(account), format);
        Path target = path(userId, exportId, format);
        write(target, content);
        return new ExportArtifact(exportId, clock.instant().plus(DOWNLOAD_TTL));
    }

    @Override
    public DownloadArtifact download(UUID userId, UUID exportId, String format) {
        requireConfigured();
        Path target = path(userId, exportId, format);
        try {
            if (!Files.isRegularFile(target)) {
                throw EarthTripException.notFound(
                    "DATA_EXPORT_FILE_NOT_FOUND",
                    "개인정보 내보내기 파일을 찾을 수 없습니다."
                );
            }
            String extension = "ZIP".equals(format) ? "zip" : "json";
            String contentType = "ZIP".equals(format)
                ? "application/zip"
                : "application/json";
            return new DownloadArtifact(
                Files.readAllBytes(target),
                contentType,
                "earth-trip-personal-data-" + exportId + "." + extension
            );
        } catch (IOException exception) {
            throw storageUnavailable();
        }
    }

    private Map<String, Object> snapshot(UserAccount account) {
        UUID userId = account.id().value();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schemaVersion", "v1");
        result.put("exportedAt", clock.instant());
        result.put("account", Map.of(
            "userId", userId,
            "email", account.email().value(),
            "displayName", account.displayName(),
            "status", account.status().name(),
            "emailVerifiedAt", nullable(account.emailVerifiedAt()),
            "createdAt", account.createdAt(),
            "updatedAt", account.updatedAt()
        ));
        result.put("linkedIdentities", identities.findByUser(userId).stream().map(identity -> Map.of(
            "provider", identity.provider(),
            "providerEmail", nullable(identity.providerEmail()),
            "createdAt", identity.createdAt(),
            "lastUsedAt", identity.lastUsedAt()
        )).toList());
        result.put("sessions", sessions.findByUserId(account.id()).stream()
            .map(LocalPersonalDataExporter::session)
            .toList());
        result.put("preferences", preferences.find(userId).map(LocalPersonalDataExporter::preference)
            .orElse(Map.of()));
        result.put("policyConsents", policies.findConsents(userId).stream().map(consent -> Map.of(
            "policyId", consent.policy().id(),
            "policyType", consent.policy().type(),
            "policyVersion", consent.policy().version(),
            "decision", consent.decision(),
            "decidedAt", consent.decidedAt(),
            "source", consent.source()
        )).toList());
        result.put("favoriteCompanions", support.favorites(userId));
        result.put("deletionRequest", deletions.findPending(account.id())
            .<Object>map(value -> value)
            .orElse(Map.of()));
        return Map.copyOf(result);
    }

    private byte[] serialize(Map<String, Object> snapshot, String format) {
        try {
            byte[] data = json.writerWithDefaultPrettyPrinter().writeValueAsBytes(snapshot);
            if (!"ZIP".equals(format)) {
                return data;
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                zip.putNextEntry(new ZipEntry("personal-data.json"));
                zip.write(data);
                zip.closeEntry();
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("개인정보 내보내기 문서를 생성할 수 없습니다.", exception);
        }
    }

    private void write(Path target, byte[] content) {
        Path temporary = null;
        try {
            Files.createDirectories(target.getParent());
            restrictDirectory(target.getParent());
            temporary = Files.createTempFile(target.getParent(), ".export-", ".tmp");
            Files.write(temporary, content);
            try {
                Files.move(
                    temporary,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                );
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            restrictFile(target);
        } catch (IOException exception) {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // 원래 저장 실패를 유지한다.
                }
            }
            throw storageUnavailable();
        }
    }

    private Path path(UUID userId, UUID exportId, String format) {
        String extension = "ZIP".equals(format) ? ".zip" : ".json";
        Path result = root.resolve("users").resolve(userId.toString())
            .resolve("exports").resolve(exportId + extension).normalize();
        if (!result.startsWith(root)) {
            throw EarthTripException.badRequest(
                "INVALID_DATA_EXPORT_PATH",
                "개인정보 내보내기 경로가 올바르지 않습니다."
            );
        }
        return result;
    }

    private void requireConfigured() {
        if (root == null) {
            throw EarthTripException.unavailable(
                "DATA_EXPORT_STORAGE_NOT_CONFIGURED",
                "개인정보 내보내기 저장 경로가 설정되지 않았습니다."
            );
        }
    }

    private static Map<String, Object> session(AuthSession session) {
        return Map.of(
            "sessionId", session.id(),
            "deviceName", session.deviceName(),
            "accessExpiresAt", session.accessExpiresAt(),
            "refreshExpiresAt", session.refreshExpiresAt(),
            "lastUsedAt", session.lastUsedAt(),
            "revokedAt", nullable(session.revokedAt()),
            "createdAt", session.createdAt()
        );
    }

    private static Map<String, Object> preference(PreferenceStorePort.PreferenceRecord preference) {
        return Map.of(
            "locale", preference.locale(),
            "defaultCurrency", preference.defaultCurrency(),
            "timeZone", preference.timeZone(),
            "shareTicketNames", preference.shareTicketNames(),
            "sharePersonalExpense", preference.sharePersonalExpense(),
            "optionalAnalytics", preference.optionalAnalytics(),
            "updatedAt", preference.updatedAt()
        );
    }

    private static Object nullable(Object value) {
        return value == null ? "" : value;
    }

    private static void restrictDirectory(Path directory) {
        try {
            Files.setPosixFilePermissions(directory, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE
            ));
        } catch (UnsupportedOperationException | IOException ignored) {
            // POSIX 권한을 지원하지 않는 개발 파일시스템에서는 OS 기본 권한을 사용한다.
        }
    }

    private static void restrictFile(Path file) {
        try {
            Files.setPosixFilePermissions(file, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            ));
        } catch (UnsupportedOperationException | IOException ignored) {
            // POSIX 권한을 지원하지 않는 개발 파일시스템에서는 OS 기본 권한을 사용한다.
        }
    }

    private static EarthTripException storageUnavailable() {
        return EarthTripException.unavailable(
            "DATA_EXPORT_STORAGE_UNAVAILABLE",
            "개인정보 내보내기 파일을 저장하거나 읽을 수 없습니다."
        );
    }
}
