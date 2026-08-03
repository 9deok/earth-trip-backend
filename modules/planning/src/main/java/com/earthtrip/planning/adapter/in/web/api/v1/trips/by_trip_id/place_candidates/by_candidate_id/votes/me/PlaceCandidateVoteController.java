package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.place_candidates.by_candidate_id.votes.me;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;import com.earthtrip.sharedkernel.security.CurrentActor;import jakarta.validation.Valid;import jakarta.validation.constraints.NotNull;import java.util.*;import org.springframework.http.HttpStatus;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/trips/{tripId}/place-candidates/{candidateId}/votes/me") class PlaceCandidateVoteController{
 private final PlanningResourceUseCase useCase;private final CurrentActor actor;PlaceCandidateVoteController(PlanningResourceUseCase u,CurrentActor a){useCase=u;actor=a;}
 @PutMapping PlanningResourceUseCase.UserStateResult put(@PathVariable UUID tripId,@PathVariable UUID candidateId,@Valid @RequestBody VoteMutation r){return useCase.putUserState(tripId,actor.requireUserId(),"PLACE_CANDIDATE",candidateId,"PREFERENCE",r.value());}
 @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable UUID tripId,@PathVariable UUID candidateId){useCase.deleteUserState(tripId,actor.requireUserId(),"PLACE_CANDIDATE",candidateId,"PREFERENCE");}
}
record VoteMutation(@NotNull Map<String,Object> value){}
