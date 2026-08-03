package com.earthtrip.platform.adapter.in.web.api.v1.files.by_file_id.upload_completions;

import com.earthtrip.platform.application.port.in.FileUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files/{fileId}/upload-completions")
class FileUploadCompletionsController {

    private final FileUseCase useCase;
    private final CurrentActor actor;

    FileUploadCompletionsController(FileUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    CompletedFileResponse post(
        @PathVariable UUID fileId,
        @Valid @RequestBody FileUploadCompletionRequest request
    ) {
        return CompletedFileResponse.from(
            useCase.complete(actor.requireUserId(), fileId, request.uploadSessionId())
        );
    }
}

record FileUploadCompletionRequest(@NotNull UUID uploadSessionId) { }

record CompletedFileResponse(
    UUID fileId,
    String fileName,
    String mimeType,
    long sizeBytes,
    String checksumSha256,
    String status,
    long version,
    Instant createdAt,
    Instant completedAt
) {
    static CompletedFileResponse from(FileUseCase.FileResult result) {
        return new CompletedFileResponse(
            result.fileId(), result.fileName(), result.mimeType(), result.sizeBytes(),
            result.checksumSha256(), result.status(), result.version(), result.createdAt(),
            result.completedAt()
        );
    }
}
