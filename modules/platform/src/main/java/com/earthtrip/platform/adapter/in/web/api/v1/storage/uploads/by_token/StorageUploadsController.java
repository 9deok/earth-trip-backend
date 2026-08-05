package com.earthtrip.platform.adapter.in.web.api.v1.storage.uploads.by_token;

import com.earthtrip.platform.application.port.in.ObjectContentUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/storage/uploads/{token}")
class StorageUploadsController {

    private final ObjectContentUseCase useCase;

    StorageUploadsController(ObjectContentUseCase useCase) {
        this.useCase = useCase;
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void put(
        @PathVariable String token,
        @RequestHeader(name = "Content-Type", required = false) String contentType,
        @RequestBody byte[] content
    ) {
        useCase.upload(token, contentType, content);
    }
}
