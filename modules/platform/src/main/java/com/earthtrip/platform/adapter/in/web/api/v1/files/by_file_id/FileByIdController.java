package com.earthtrip.platform.adapter.in.web.api.v1.files.by_file_id;

import com.earthtrip.platform.application.port.in.FileUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files/{fileId}")
class FileByIdController {

    private final FileUseCase useCase;
    private final CurrentActor actor;

    FileByIdController(FileUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    FileMetadataResponse get(@PathVariable UUID fileId) {
        return FileMetadataResponse.from(useCase.get(actor.requireUserId(), fileId));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID fileId, @RequestParam long baseVersion) {
        useCase.delete(actor.requireUserId(), fileId, baseVersion);
    }
}

record FileMetadataResponse(
        UUID fileId,
        String fileName,
        String mimeType,
        long sizeBytes,
        String checksumSha256,
        String status,
        long version,
        Instant createdAt,
        Instant completedAt) {
    static FileMetadataResponse from(FileUseCase.FileResult result) {
        return new FileMetadataResponse(
                result.fileId(),
                result.fileName(),
                result.mimeType(),
                result.sizeBytes(),
                result.checksumSha256(),
                result.status(),
                result.version(),
                result.createdAt(),
                result.completedAt());
    }
}
