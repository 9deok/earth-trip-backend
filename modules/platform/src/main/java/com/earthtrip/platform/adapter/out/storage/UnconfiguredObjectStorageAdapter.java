package com.earthtrip.platform.adapter.out.storage;

import com.earthtrip.platform.application.port.out.ObjectStoragePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import org.springframework.stereotype.Component;

@Component
class UnconfiguredObjectStorageAdapter implements ObjectStoragePort {

    @Override
    public SignedUrl upload(String storageKey, String mimeType, long sizeBytes, String checksum) {
        throw unavailable();
    }

    @Override
    public void verifyUpload(String storageKey, String mimeType, long sizeBytes, String checksum) {
        throw unavailable();
    }

    @Override
    public SignedUrl download(String storageKey) {
        throw unavailable();
    }

    @Override
    public void delete(String storageKey) {
        throw unavailable();
    }

    private static EarthTripException unavailable() {
        return EarthTripException.unavailable(
            "OBJECT_STORAGE_NOT_CONFIGURED",
            "파일 저장소가 설정되지 않았습니다."
        );
    }
}
