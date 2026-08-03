package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.days.by_day_id.schedule_items;
import com.earthtrip.planning.application.port.in.*;import com.earthtrip.sharedkernel.security.CurrentActor;import jakarta.validation.Valid;import jakarta.validation.constraints.NotNull;import java.util.*;import org.springframework.http.HttpStatus;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/trips/{tripId}/days/{dayId}/schedule-items") class ScheduleItemsController{
 private final ScheduleUseCase useCase;private final CurrentActor actor;ScheduleItemsController(ScheduleUseCase u,CurrentActor a){useCase=u;actor=a;}
 @GetMapping List<PlanningResourceUseCase.ResourceResult> get(@PathVariable UUID tripId,@PathVariable UUID dayId){return useCase.list(tripId,dayId,actor.requireUserId());}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) PlanningResourceUseCase.ResourceResult post(@PathVariable UUID tripId,@PathVariable UUID dayId,@Valid @RequestBody ScheduleMutation r){return useCase.create(tripId,dayId,actor.requireUserId(),new PlanningResourceUseCase.ResourceCommand(r.requestId(),dayId,null,r.payload(),r.status(),r.sortOrder(),0));}
}
record ScheduleMutation(@NotNull UUID requestId,@NotNull Map<String,Object> payload,String status,Integer sortOrder){}
