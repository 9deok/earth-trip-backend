package com.earthtrip.platform.adapter.in.web.api.v1.files.upload_sessions.by_upload_session_id;

import com.earthtrip.platform.application.port.in.FileUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files/upload-sessions/{uploadSessionId}")
class FileUploadSessionByIdController {

    private final FileUseCase useCase;
    private final CurrentActor actor;

    FileUploadSessionByIdController(FileUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    FileUploadSessionStatusResponse get(@PathVariable UUID uploadSessionId) {
        return FileUploadSessionStatusResponse.from(
            useCase.uploadStatus(actor.requireUserId(), uploadSessionId)
        );
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID uploadSessionId) {
        useCase.abortUpload(actor.requireUserId(), uploadSessionId);
    }
}

record FileUploadSessionStatusResponse(
    UUID uploadSessionId,
    UUID fileId,
    String status,
    Instant expiresAt
) {
    static FileUploadSessionStatusResponse from(FileUseCase.UploadResult result) {
        return new FileUploadSessionStatusResponse(
            result.uploadSessionId(), result.fileId(), result.status(), result.expiresAt()
        );
    }
}
