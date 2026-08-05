package com.earthtrip.platform.adapter.out.storage;

import com.earthtrip.platform.application.port.out.ObjectContentPort;
import com.earthtrip.platform.application.port.out.ObjectStoragePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class LocalFileObjectStorageAdapter implements ObjectStoragePort, ObjectContentPort {

    private static final Duration SIGNED_URL_TTL = Duration.ofMinutes(15);
    private static final int MAX_FILE_BYTES = 25 * 1024 * 1024;

    private final Path root;
    private final String backendBaseUrl;
    private final byte[] signingKey;
    private final Clock clock;

    LocalFileObjectStorageAdapter(
        @Value("${earthtrip.storage.local.root:}") String root,
        @Value("${earthtrip.backend-public-base-url:http://localhost:8080}") String backendBaseUrl,
        @Value("${earthtrip.storage.local.signing-key:}") String signingKey,
        Clock clock
    ) {
        this.root = root == null || root.isBlank()
            ? null
            : Path.of(root).toAbsolutePath().normalize();
        this.backendBaseUrl = backendBaseUrl == null
            ? "http://localhost:8080"
            : backendBaseUrl.strip().replaceAll("/+$", "");
        this.signingKey = decodeKey(signingKey);
        this.clock = clock;
    }

    @Override
    public SignedUrl upload(String storageKey, String mimeType, long sizeBytes, String checksum) {
        requireConfigured();
        if (sizeBytes < 0 || sizeBytes > MAX_FILE_BYTES) {
            throw EarthTripException.badRequest("INVALID_FILE_SIZE", "파일 크기를 확인해 주세요.");
        }
        Instant expiresAt = clock.instant().plus(SIGNED_URL_TTL);
        String token = sign(new Claim(
            "UPLOAD", expiresAt.getEpochSecond(), storageKey, mimeType, sizeBytes, checksum
        ));
        return new SignedUrl(
            backendBaseUrl + "/api/v1/storage/uploads/" + token,
            expiresAt
        );
    }

    @Override
    public void verifyUpload(
        String storageKey,
        String mimeType,
        long sizeBytes,
        String checksum
    ) {
        requireConfigured();
        Path file = path(storageKey);
        try {
            if (!Files.isRegularFile(file)
                || Files.size(file) != sizeBytes
                || !checksum.equalsIgnoreCase(sha256(file))) {
                throw EarthTripException.conflict(
                    "UPLOADED_FILE_MISMATCH",
                    "업로드된 파일의 크기 또는 체크섬이 요청과 일치하지 않습니다."
                );
            }
        } catch (IOException exception) {
            throw storageUnavailable();
        }
    }

    @Override
    public SignedUrl download(String storageKey) {
        requireConfigured();
        Path file = path(storageKey);
        if (!Files.isRegularFile(file)) {
            throw EarthTripException.notFound("STORED_FILE_NOT_FOUND", "저장된 파일을 찾을 수 없습니다.");
        }
        Instant expiresAt = clock.instant().plus(SIGNED_URL_TTL);
        String contentType = storedContentType(file);
        String token = sign(new Claim(
            "DOWNLOAD", expiresAt.getEpochSecond(), storageKey,
            contentType == null ? "application/octet-stream" : contentType,
            -1, ""
        ));
        return new SignedUrl(
            backendBaseUrl + "/api/v1/storage/downloads/" + token,
            expiresAt
        );
    }

    @Override
    public void delete(String storageKey) {
        requireConfigured();
        try {
            Path file = path(storageKey);
            Files.deleteIfExists(file);
            Files.deleteIfExists(metadataPath(file));
        } catch (IOException exception) {
            throw storageUnavailable();
        }
    }

    @Override
    public void write(String signedToken, String contentType, byte[] content) {
        Claim claim = verify(signedToken, "UPLOAD");
        String normalizedContentType = normalizeContentType(contentType);
        if (!claim.contentType().equalsIgnoreCase(normalizedContentType)) {
            throw EarthTripException.badRequest(
                "UPLOAD_CONTENT_TYPE_MISMATCH",
                "업로드 Content-Type이 발급된 세션과 일치하지 않습니다."
            );
        }
        if (content.length != claim.sizeBytes()) {
            throw EarthTripException.badRequest(
                "UPLOAD_SIZE_MISMATCH",
                "업로드 크기가 발급된 세션과 일치하지 않습니다."
            );
        }
        String digest = sha256(content);
        if (!MessageDigest.isEqual(
            digest.getBytes(StandardCharsets.US_ASCII),
            claim.checksum().toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII)
        )) {
            throw EarthTripException.badRequest(
                "UPLOAD_CHECKSUM_MISMATCH",
                "업로드 체크섬이 발급된 세션과 일치하지 않습니다."
            );
        }
        Path target = path(claim.storageKey());
        Path temporary = null;
        try {
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
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
            Files.writeString(
                metadataPath(target),
                claim.contentType(),
                StandardCharsets.UTF_8
            );
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

    @Override
    public StoredContent read(String signedToken) {
        Claim claim = verify(signedToken, "DOWNLOAD");
        Path file = path(claim.storageKey());
        try {
            if (!Files.isRegularFile(file)) {
                throw EarthTripException.notFound(
                    "STORED_FILE_NOT_FOUND",
                    "저장된 파일을 찾을 수 없습니다."
                );
            }
            return new StoredContent(Files.readAllBytes(file), claim.contentType());
        } catch (IOException exception) {
            throw storageUnavailable();
        }
    }

    private String sign(Claim claim) {
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            claim.serialize().getBytes(StandardCharsets.UTF_8)
        );
        return payload + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(payload));
    }

    private Claim verify(String token, String action) {
        requireConfigured();
        if (token == null || token.isBlank()) {
            throw invalidToken();
        }
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) {
            throw invalidToken();
        }
        byte[] supplied;
        String serialized;
        try {
            supplied = Base64.getUrlDecoder().decode(parts[1]);
            serialized = new String(
                Base64.getUrlDecoder().decode(parts[0]),
                StandardCharsets.UTF_8
            );
        } catch (IllegalArgumentException exception) {
            throw invalidToken();
        }
        if (!MessageDigest.isEqual(hmac(parts[0]), supplied)) {
            throw invalidToken();
        }
        Claim claim = Claim.parse(serialized);
        if (!action.equals(claim.action()) || claim.expiresAtEpoch() < clock.instant().getEpochSecond()) {
            throw invalidToken();
        }
        return claim;
    }

    private byte[] hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.US_ASCII));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("로컬 파일 서명을 생성할 수 없습니다.", exception);
        }
    }

    private Path path(String storageKey) {
        if (storageKey == null || storageKey.isBlank() || storageKey.indexOf('\0') >= 0) {
            throw invalidStorageKey();
        }
        Path result = root.resolve(storageKey).normalize();
        if (!result.startsWith(root)) {
            throw invalidStorageKey();
        }
        return result;
    }

    private static Path metadataPath(Path file) {
        return file.resolveSibling(file.getFileName() + ".content-type");
    }

    private static String storedContentType(Path file) {
        try {
            Path metadata = metadataPath(file);
            if (Files.isRegularFile(metadata)) {
                String value = normalizeContentType(Files.readString(metadata, StandardCharsets.UTF_8));
                if (!value.isBlank()) {
                    return value;
                }
            }
            String detected = Files.probeContentType(file);
            return detected == null ? "application/octet-stream" : detected;
        } catch (IOException exception) {
            return "application/octet-stream";
        }
    }

    private void requireConfigured() {
        if (root == null || signingKey.length < 32) {
            throw EarthTripException.unavailable(
                "OBJECT_STORAGE_NOT_CONFIGURED",
                "로컬 파일 루트와 서명 키가 설정되지 않았습니다."
            );
        }
    }

    private static String normalizeContentType(String value) {
        if (value == null) {
            return "application/octet-stream";
        }
        return value.split(";", 2)[0].strip().toLowerCase(java.util.Locale.ROOT);
    }

    private static byte[] decodeKey(String value) {
        try {
            return value == null || value.isBlank()
                ? new byte[0]
                : Base64.getDecoder().decode(value.strip());
        } catch (IllegalArgumentException exception) {
            return new byte[0];
        }
    }

    private static String sha256(Path file) throws IOException {
        try (java.io.InputStream input = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static EarthTripException invalidToken() {
        return EarthTripException.unauthorized(
            "INVALID_STORAGE_TOKEN",
            "파일 전송 토큰이 만료되었거나 올바르지 않습니다."
        );
    }

    private static EarthTripException invalidStorageKey() {
        return EarthTripException.badRequest(
            "INVALID_STORAGE_KEY",
            "파일 저장 경로가 올바르지 않습니다."
        );
    }

    private static EarthTripException storageUnavailable() {
        return EarthTripException.unavailable(
            "OBJECT_STORAGE_UNAVAILABLE",
            "로컬 파일 저장소를 사용할 수 없습니다."
        );
    }

    private record Claim(
        String action,
        long expiresAtEpoch,
        String storageKey,
        String contentType,
        long sizeBytes,
        String checksum
    ) {
        String serialize() {
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            return action + ":" + expiresAtEpoch + ":"
                + encode(encoder, storageKey) + ":"
                + encode(encoder, contentType) + ":"
                + sizeBytes + ":" + checksum;
        }

        static Claim parse(String value) {
            String[] parts = value.split(":", 6);
            if (parts.length != 6) {
                throw invalidToken();
            }
            try {
                Base64.Decoder decoder = Base64.getUrlDecoder();
                return new Claim(
                    parts[0],
                    Long.parseLong(parts[1]),
                    decode(decoder, parts[2]),
                    decode(decoder, parts[3]),
                    Long.parseLong(parts[4]),
                    parts[5]
                );
            } catch (IllegalArgumentException exception) {
                throw invalidToken();
            }
        }

        private static String encode(Base64.Encoder encoder, String value) {
            return encoder.encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }

        private static String decode(Base64.Decoder decoder, String value) {
            return new String(decoder.decode(value), StandardCharsets.UTF_8);
        }
    }
}
