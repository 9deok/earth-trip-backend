package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.research_sources;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;import com.earthtrip.sharedkernel.security.CurrentActor;import jakarta.validation.Valid;import jakarta.validation.constraints.NotNull;import java.util.*;import org.springframework.http.HttpStatus;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/trips/{tripId}/research-sources") class ResearchSourcesController{
 private final PlanningResourceUseCase useCase;private final CurrentActor actor;ResearchSourcesController(PlanningResourceUseCase u,CurrentActor a){useCase=u;actor=a;}
 @GetMapping List<PlanningResourceUseCase.ResourceResult> get(@PathVariable UUID tripId){return useCase.list(tripId,actor.requireUserId(),"RESEARCH_SOURCE",null,null);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) PlanningResourceUseCase.ResourceResult post(@PathVariable UUID tripId,@Valid @RequestBody SourceMutation r){return useCase.create(tripId,actor.requireUserId(),"RESEARCH_SOURCE",PlanningResourceUseCase.WritePermission.EDITOR,new PlanningResourceUseCase.ResourceCommand(r.requestId(),r.categoryId(),null,r.payload(),"ACTIVE",r.sortOrder(),0));}
}
record SourceMutation(@NotNull UUID requestId,UUID categoryId,@NotNull Map<String,Object> payload,Integer sortOrder){}
