package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.place_candidates.by_candidate_id;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@SuppressWarnings("auxiliaryclass")
class PlaceCandidateByIdControllerTest {

    @Test
    void 후보_수정과_삭제는_작성자_전용_권한으로_요청한다() {
        PlanningResourceUseCase useCase = mock(PlanningResourceUseCase.class);
        CurrentActor actor = mock(CurrentActor.class);
        UUID tripId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(actor.requireUserId()).thenReturn(actorId);
        PlaceCandidateByIdController controller = new PlaceCandidateByIdController(useCase, actor);

        controller.patch(
                tripId,
                candidateId,
                new PlaceMutation(Map.of("title", "수정한 후보"), "PROPOSED", 0, 3));
        controller.delete(tripId, candidateId, new PlaceDelete(4));

        verify(useCase)
                .update(
                        eq(tripId),
                        eq(actorId),
                        eq("PLACE_CANDIDATE"),
                        eq(candidateId),
                        eq(PlanningResourceUseCase.WritePermission.MEMBER),
                        any(PlanningResourceUseCase.ResourceCommand.class));
        verify(useCase)
                .delete(
                        tripId,
                        actorId,
                        "PLACE_CANDIDATE",
                        candidateId,
                        PlanningResourceUseCase.WritePermission.MEMBER,
                        4);
    }
}
