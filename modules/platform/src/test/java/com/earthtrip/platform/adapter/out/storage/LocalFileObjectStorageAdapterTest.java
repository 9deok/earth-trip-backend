package com.earthtrip.platform.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.earthtrip.platform.application.port.out.ObjectContentPort;
import com.earthtrip.platform.application.port.out.ObjectStoragePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileObjectStorageAdapterTest {

    @TempDir
    Path root;

    @Test
    void signedUploadVerifiesChecksumAndPreservesContentTypeForDownload() throws Exception {
        LocalFileObjectStorageAdapter adapter = adapter();
        byte[] content = "earth-trip-file".getBytes(StandardCharsets.UTF_8);
        String checksum = HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(content)
        );
        String storageKey = "users/user-1/files/file-1";

        ObjectStoragePort.SignedUrl upload = adapter.upload(
            storageKey,
            "text/plain",
            content.length,
            checksum
        );
        adapter.write(token(upload.url()), "text/plain; charset=UTF-8", content);
        adapter.verifyUpload(storageKey, "text/plain", content.length, checksum);

        ObjectStoragePort.SignedUrl download = adapter.download(storageKey);
        ObjectContentPort.StoredContent stored = adapter.read(token(download.url()));

        assertThat(stored.content()).isEqualTo(content);
        assertThat(stored.contentType()).isEqualTo("text/plain");
    }

    @Test
    void rejectsTamperedSignedToken() {
        LocalFileObjectStorageAdapter adapter = adapter();

        assertThatThrownBy(() -> adapter.read("tampered.token"))
            .isInstanceOfSatisfying(EarthTripException.class, error ->
                assertThat(error.code()).isEqualTo("INVALID_STORAGE_TOKEN")
            );
    }

    private LocalFileObjectStorageAdapter adapter() {
        String signingKey = Base64.getEncoder().encodeToString(
            "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)
        );
        return new LocalFileObjectStorageAdapter(
            root.toString(),
            "https://api.earthtrip.test",
            signingKey,
            Clock.fixed(Instant.parse("2026-08-03T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    private static String token(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }
}
