package com.earthtrip.identity.adapter.in.web.api.v1.me.data_exports.by_export_id.download;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.DataExportDownloadUseCase;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/data-exports/{exportId}/download")
class DataExportDownloadController {

    private final DataExportDownloadUseCase useCase;
    private final CurrentUserProvider currentUser;

    DataExportDownloadController(
            DataExportDownloadUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @GetMapping
    ResponseEntity<byte[]> get(@PathVariable UUID exportId) {
        DataExportDownloadUseCase.DownloadResult result =
                useCase.download(currentUser.requireUserId(), exportId);
        String disposition = "attachment; filename=\"" + result.fileName() + "\"";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.content().length)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff")
                .body(result.content());
    }
}
