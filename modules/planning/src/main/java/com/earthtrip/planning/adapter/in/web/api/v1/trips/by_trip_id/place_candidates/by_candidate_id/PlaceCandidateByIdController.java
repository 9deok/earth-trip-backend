package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.place_candidates.by_candidate_id;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;import com.earthtrip.sharedkernel.security.CurrentActor;import jakarta.validation.Valid;import jakarta.validation.constraints.Min;import java.util.*;import org.springframework.http.HttpStatus;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/trips/{tripId}/place-candidates/{candidateId}") class PlaceCandidateByIdController{
 private final PlanningResourceUseCase useCase;private final CurrentActor actor;PlaceCandidateByIdController(PlanningResourceUseCase u,CurrentActor a){useCase=u;actor=a;}
 @GetMapping PlanningResourceUseCase.ResourceResult get(@PathVariable UUID tripId,@PathVariable UUID candidateId){return useCase.get(tripId,actor.requireUserId(),"PLACE_CANDIDATE",candidateId);}
 @PatchMapping PlanningResourceUseCase.ResourceResult patch(@PathVariable UUID tripId,@PathVariable UUID candidateId,@Valid @RequestBody PlaceMutation r){return useCase.update(tripId,actor.requireUserId(),"PLACE_CANDIDATE",candidateId,PlanningResourceUseCase.WritePermission.EDITOR,new PlanningResourceUseCase.ResourceCommand(candidateId,null,null,r.payload(),r.status(),r.sortOrder(),r.baseVersion()));}
 @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable UUID tripId,@PathVariable UUID candidateId,@Valid @RequestBody PlaceDelete r){useCase.delete(tripId,actor.requireUserId(),"PLACE_CANDIDATE",candidateId,PlanningResourceUseCase.WritePermission.EDITOR,r.baseVersion());}
}
record PlaceMutation(Map<String,Object> payload,String status,Integer sortOrder,@Min(0) long baseVersion){}record PlaceDelete(@Min(0) long baseVersion){}
