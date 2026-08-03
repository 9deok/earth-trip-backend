package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.comparison_options.by_option_id;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;import com.earthtrip.sharedkernel.security.CurrentActor;import jakarta.validation.Valid;import jakarta.validation.constraints.Min;import java.util.*;import org.springframework.http.HttpStatus;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/trips/{tripId}/comparison-options/{optionId}") class ComparisonOptionByIdController{
 private final PlanningResourceUseCase useCase;private final CurrentActor actor;ComparisonOptionByIdController(PlanningResourceUseCase u,CurrentActor a){useCase=u;actor=a;}
 @GetMapping PlanningResourceUseCase.ResourceResult get(@PathVariable UUID tripId,@PathVariable UUID optionId){return useCase.get(tripId,actor.requireUserId(),"COMPARISON_OPTION",optionId);}
 @PatchMapping PlanningResourceUseCase.ResourceResult patch(@PathVariable UUID tripId,@PathVariable UUID optionId,@Valid @RequestBody ComparisonMutation r){return useCase.update(tripId,actor.requireUserId(),"COMPARISON_OPTION",optionId,PlanningResourceUseCase.WritePermission.EDITOR,new PlanningResourceUseCase.ResourceCommand(optionId,null,null,r.payload(),r.status(),r.sortOrder(),r.baseVersion()));}
 @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable UUID tripId,@PathVariable UUID optionId,@Valid @RequestBody ComparisonDelete r){useCase.delete(tripId,actor.requireUserId(),"COMPARISON_OPTION",optionId,PlanningResourceUseCase.WritePermission.EDITOR,r.baseVersion());}
}
record ComparisonMutation(Map<String,Object> payload,String status,Integer sortOrder,@Min(0) long baseVersion){}record ComparisonDelete(@Min(0) long baseVersion){}
