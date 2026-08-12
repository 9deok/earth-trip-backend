package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.place_candidates.by_candidate_id.comments.by_comment_id;

import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/place-candidates/{candidateId}/comments/{commentId}")
class PlaceCommentByIdController {
    private final PlanningResourceUseCase useCase;
    private final CurrentActor actor;

    PlaceCommentByIdController(PlanningResourceUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @PatchMapping
    PlanningResourceUseCase.ResourceResult patch(
            @PathVariable UUID tripId,
            @PathVariable UUID candidateId,
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentMutation r) {
        return useCase.update(
                tripId,
                actor.requireUserId(),
                "PLACE_COMMENT",
                commentId,
                PlanningResourceUseCase.WritePermission.MEMBER,
                new PlanningResourceUseCase.ResourceCommand(
                        commentId, candidateId, null, r.payload(), null, null, r.baseVersion()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @PathVariable UUID tripId,
            @PathVariable UUID candidateId,
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentDelete r) {
        useCase.delete(
                tripId,
                actor.requireUserId(),
                "PLACE_COMMENT",
                commentId,
                PlanningResourceUseCase.WritePermission.MEMBER,
                r.baseVersion());
    }
}

record CommentMutation(Map<String, Object> payload, @Min(0) long baseVersion) {}

record CommentDelete(@Min(0) long baseVersion) {}
