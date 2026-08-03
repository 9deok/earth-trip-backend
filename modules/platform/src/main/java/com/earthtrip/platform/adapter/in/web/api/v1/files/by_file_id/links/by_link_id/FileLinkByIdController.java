package com.earthtrip.platform.adapter.in.web.api.v1.files.by_file_id.links.by_link_id;

import com.earthtrip.platform.application.port.in.FileUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files/{fileId}/links/{linkId}")
class FileLinkByIdController {

    private final FileUseCase useCase;
    private final CurrentActor actor;

    FileLinkByIdController(FileUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID fileId, @PathVariable UUID linkId) {
        useCase.unlink(actor.requireUserId(), fileId, linkId);
    }
}
