package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.polls.by_poll_id;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;import com.earthtrip.sharedkernel.security.CurrentActor;import jakarta.validation.Valid;import jakarta.validation.constraints.Min;import java.util.*;import org.springframework.http.HttpStatus;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/trips/{tripId}/polls/{pollId}") class PollByIdController{
 private final PlanningResourceUseCase useCase;private final CurrentActor actor;PollByIdController(PlanningResourceUseCase u,CurrentActor a){useCase=u;actor=a;}
 @GetMapping PlanningResourceUseCase.ResourceResult get(@PathVariable UUID tripId,@PathVariable UUID pollId){return useCase.get(tripId,actor.requireUserId(),"POLL",pollId);}
 @PatchMapping PlanningResourceUseCase.ResourceResult patch(@PathVariable UUID tripId,@PathVariable UUID pollId,@Valid @RequestBody PollMutation r){return useCase.update(tripId,actor.requireUserId(),"POLL",pollId,PlanningResourceUseCase.WritePermission.EDITOR,new PlanningResourceUseCase.ResourceCommand(pollId,null,null,r.payload(),r.status(),null,r.baseVersion()));}
 @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable UUID tripId,@PathVariable UUID pollId,@Valid @RequestBody PollDelete r){useCase.delete(tripId,actor.requireUserId(),"POLL",pollId,PlanningResourceUseCase.WritePermission.EDITOR,r.baseVersion());}
}
record PollMutation(Map<String,Object> payload,String status,@Min(0) long baseVersion){}record PollDelete(@Min(0) long baseVersion){}
