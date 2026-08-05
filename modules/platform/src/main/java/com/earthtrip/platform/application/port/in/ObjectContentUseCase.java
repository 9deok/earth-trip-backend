package com.earthtrip.platform.application.port.in;

public interface ObjectContentUseCase {

    void upload(String signedToken, String contentType, byte[] content);

    ContentResult download(String signedToken);

    record ContentResult(byte[] content, String contentType) {
        public ContentResult {
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }
}
