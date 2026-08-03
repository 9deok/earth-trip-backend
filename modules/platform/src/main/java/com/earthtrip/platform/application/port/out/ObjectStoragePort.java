package com.earthtrip.platform.application.port.out;

import java.time.Instant;

public interface ObjectStoragePort {

    SignedUrl upload(String storageKey, String mimeType, long sizeBytes, String checksum);

    void verifyUpload(String storageKey, String mimeType, long sizeBytes, String checksum);

    SignedUrl download(String storageKey);

    void delete(String storageKey);

    record SignedUrl(String url, Instant expiresAt) { }
}
