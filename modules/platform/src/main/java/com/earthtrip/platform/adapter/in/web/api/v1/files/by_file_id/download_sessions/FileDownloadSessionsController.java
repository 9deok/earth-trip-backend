package com.earthtrip.platform.adapter.in.web.api.v1.files.by_file_id.download_sessions;

import com.earthtrip.platform.application.port.in.FileUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files/{fileId}/download-sessions")
class FileDownloadSessionsController {

    private final FileUseCase useCase;
    private final CurrentActor actor;

    FileDownloadSessionsController(FileUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    FileDownloadSessionResponse post(@PathVariable UUID fileId) {
        FileUseCase.DownloadResult result = useCase.download(actor.requireUserId(), fileId);
        return new FileDownloadSessionResponse(
            result.fileId(), result.downloadUrl(), result.expiresAt()
        );
    }
}

record FileDownloadSessionResponse(UUID fileId, String downloadUrl, Instant expiresAt) { }
