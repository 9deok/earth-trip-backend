package com.earthtrip.platform.application.service.file;

import com.earthtrip.platform.application.port.in.ObjectContentUseCase;
import com.earthtrip.platform.application.port.out.ObjectContentPort;
import org.springframework.stereotype.Service;

@Service
class ObjectContentService implements ObjectContentUseCase {

    private final ObjectContentPort contentPort;

    ObjectContentService(ObjectContentPort contentPort) {
        this.contentPort = contentPort;
    }

    @Override
    public void upload(String signedToken, String contentType, byte[] content) {
        contentPort.write(signedToken, contentType, content == null ? new byte[0] : content);
    }

    @Override
    public ContentResult download(String signedToken) {
        ObjectContentPort.StoredContent stored = contentPort.read(signedToken);
        return new ContentResult(stored.content(), stored.contentType());
    }
}
