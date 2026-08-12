package com.earthtrip.platform.adapter.in.web.api.v1.files.upload_sessions;

import com.earthtrip.platform.application.port.in.FileUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files/upload-sessions")
class FileUploadSessionsController {

    private final FileUseCase useCase;
    private final CurrentActor actor;

    FileUploadSessionsController(FileUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    FileUploadSessionResponse post(@Valid @RequestBody FileUploadSessionRequest request) {
        FileUseCase.UploadResult result =
                useCase.createUpload(
                        actor.requireUserId(),
                        request.requestId(),
                        request.fileName(),
                        request.mimeType(),
                        request.sizeBytes(),
                        request.checksumSha256());
        return FileUploadSessionResponse.from(result);
    }
}

record FileUploadSessionRequest(
        @NotNull UUID requestId,
        @NotBlank String fileName,
        @NotBlank String mimeType,
        @Positive long sizeBytes,
        @NotBlank String checksumSha256) {}

record FileUploadSessionResponse(
        UUID uploadSessionId, UUID fileId, String status, String uploadUrl, Instant expiresAt) {
    static FileUploadSessionResponse from(FileUseCase.UploadResult result) {
        return new FileUploadSessionResponse(
                result.uploadSessionId(),
                result.fileId(),
                result.status(),
                result.uploadUrl(),
                result.expiresAt());
    }
}
