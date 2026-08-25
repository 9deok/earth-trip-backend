package com.earthtrip.platform.adapter.in.web.api.v1.public_trips.by_publication_id.comments;

import com.earthtrip.platform.application.port.in.PublicTripEngagementUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public-trips/{publicationId}/comments")
class PublicTripCommentsController {
    private final PublicTripEngagementUseCase useCase;
    private final CurrentActor actor;

    PublicTripCommentsController(PublicTripEngagementUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<PublicTripEngagementUseCase.CommentResult> get(
            @PathVariable UUID publicationId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return useCase.comments(publicationId, actor.currentUserId().orElse(null), limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PublicTripEngagementUseCase.CommentResult post(
            @PathVariable UUID publicationId,
            @Valid @RequestBody PublicTripCommentRequest request) {
        return useCase.addComment(publicationId, actor.requireUserId(), request.body());
    }
}

record PublicTripCommentRequest(@NotBlank @Size(max = 800) String body) {}
