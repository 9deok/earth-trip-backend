package com.earthtrip.identity.application.port.in;

import java.util.UUID;

public interface DataExportDownloadUseCase {

    DownloadResult download(UUID actorUserId, UUID exportId);

    record DownloadResult(byte[] content, String contentType, String fileName) {
        public DownloadResult {
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }
}
