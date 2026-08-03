package com.earthtrip.platform.adapter.in.web.api.v1.trips.by_trip_id.exports.by_export_id.download;

import com.earthtrip.platform.application.port.in.TripExportUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/exports/{exportId}/download")
class TripExportDownloadController {

    private final TripExportUseCase useCase;
    private final CurrentActor actor;

    TripExportDownloadController(TripExportUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    ResponseEntity<byte[]> download(
        @PathVariable UUID tripId,
        @PathVariable UUID exportId
    ) {
        TripExportUseCase.ArtifactResult artifact = useCase.artifact(
            tripId, exportId, actor.requireUserId()
        );
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(artifact.mimeType()))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename(artifact.fileName(), StandardCharsets.UTF_8)
                    .build().toString()
            )
            .body(artifact.content());
    }
}
