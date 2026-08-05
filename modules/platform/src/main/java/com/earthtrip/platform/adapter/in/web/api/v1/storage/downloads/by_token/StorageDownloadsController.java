package com.earthtrip.platform.adapter.in.web.api.v1.storage.downloads.by_token;

import com.earthtrip.platform.application.port.in.ObjectContentUseCase;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/storage/downloads/{token}")
class StorageDownloadsController {

    private final ObjectContentUseCase useCase;

    StorageDownloadsController(ObjectContentUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    ResponseEntity<byte[]> get(@PathVariable String token) {
        ObjectContentUseCase.ContentResult result = useCase.download(token);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(result.contentType()))
            .contentLength(result.content().length)
            .cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options", "nosniff")
            .body(result.content());
    }
}
