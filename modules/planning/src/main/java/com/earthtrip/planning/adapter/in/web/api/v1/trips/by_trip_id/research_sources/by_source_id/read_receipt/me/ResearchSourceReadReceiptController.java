package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.research_sources.by_source_id.read_receipt.me;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;import com.earthtrip.sharedkernel.security.CurrentActor;import java.util.*;import org.springframework.http.HttpStatus;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/trips/{tripId}/research-sources/{sourceId}/read-receipt/me") class ResearchSourceReadReceiptController{
 private final PlanningResourceUseCase useCase;private final CurrentActor actor;ResearchSourceReadReceiptController(PlanningResourceUseCase u,CurrentActor a){useCase=u;actor=a;}
 @PutMapping PlanningResourceUseCase.UserStateResult put(@PathVariable UUID tripId,@PathVariable UUID sourceId){return useCase.putUserState(tripId,actor.requireUserId(),"RESEARCH_SOURCE",sourceId,"READ_RECEIPT",Map.of("read",true));}
 @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable UUID tripId,@PathVariable UUID sourceId){useCase.deleteUserState(tripId,actor.requireUserId(),"RESEARCH_SOURCE",sourceId,"READ_RECEIPT");}
}
