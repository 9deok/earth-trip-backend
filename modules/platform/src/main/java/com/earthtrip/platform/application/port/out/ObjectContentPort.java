package com.earthtrip.platform.application.port.out;

public interface ObjectContentPort {

    void write(String signedToken, String contentType, byte[] content);

    StoredContent read(String signedToken);

    record StoredContent(byte[] content, String contentType) {
        public StoredContent {
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }
}
