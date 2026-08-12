package com.earthtrip.platform.adapter.in.web.api.v1.files.by_file_id.links;

import com.earthtrip.platform.application.port.in.FileUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files/{fileId}/links")
class FileLinksController {

    private final FileUseCase useCase;
    private final CurrentActor actor;

    FileLinksController(FileUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<FileLinkResponse> get(@PathVariable UUID fileId) {
        return useCase.links(actor.requireUserId(), fileId).stream()
                .map(FileLinkResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    FileLinkResponse post(@PathVariable UUID fileId, @Valid @RequestBody FileLinkRequest request) {
        return FileLinkResponse.from(
                useCase.link(
                        actor.requireUserId(),
                        fileId,
                        request.requestId(),
                        request.tripId(),
                        request.resourceType(),
                        request.resourceId(),
                        request.visibility()));
    }
}

record FileLinkRequest(
        @NotNull UUID requestId,
        @NotNull UUID tripId,
        @NotBlank String resourceType,
        @NotNull UUID resourceId,
        String visibility) {}

record FileLinkResponse(
        UUID linkId,
        UUID fileId,
        UUID tripId,
        String resourceType,
        UUID resourceId,
        String visibility,
        UUID linkedBy,
        Instant linkedAt) {
    static FileLinkResponse from(FileUseCase.LinkResult result) {
        return new FileLinkResponse(
                result.linkId(),
                result.fileId(),
                result.tripId(),
                result.resourceType(),
                result.resourceId(),
                result.visibility(),
                result.linkedBy(),
                result.linkedAt());
    }
}
