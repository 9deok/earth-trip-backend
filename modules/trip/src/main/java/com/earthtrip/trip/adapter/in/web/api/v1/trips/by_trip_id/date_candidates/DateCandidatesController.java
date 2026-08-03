package com.earthtrip.trip.adapter.in.web.api.v1.trips.by_trip_id.date_candidates;
import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.DateCandidateUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/trips/{tripId}/date-candidates") class DateCandidatesController{
    private final DateCandidateUseCase useCase;private final CurrentActor actor;
    DateCandidatesController(DateCandidateUseCase u,CurrentActor a){useCase=u;actor=a;}
    @GetMapping List<DateCandidateResponse> get(@PathVariable UUID tripId){return useCase.list(tripId,actor.requireUserId()).stream().map(DateCandidatesController::response).toList();}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) DateCandidateResponse post(@PathVariable UUID tripId,@Valid @RequestBody DateCandidateRequest r){return response(useCase.create(tripId,actor.requireUserId(),new DateCandidateUseCase.CandidateCommand(r.requestId(),r.startDate(),r.endDate(),r.note(),null,0)));}
    static DateCandidateResponse response(DateCandidateUseCase.CandidateResult d){return new DateCandidateResponse(d.candidateId(),d.tripId(),d.startDate(),d.endDate(),d.note(),d.status(),d.availability().stream().map(a->new AvailabilityResponse(a.userId(),a.availability(),a.note(),a.updatedAt())).toList(),d.version(),d.createdBy(),d.createdAt(),d.updatedAt());}
}
record DateCandidateRequest(@NotNull UUID requestId,@NotNull LocalDate startDate,@NotNull LocalDate endDate,String note){}
record AvailabilityResponse(UUID userId,String availability,String note,Instant updatedAt){}
record DateCandidateResponse(UUID candidateId,UUID tripId,LocalDate startDate,LocalDate endDate,String note,String status,List<AvailabilityResponse> availability,long version,UUID createdBy,Instant createdAt,Instant updatedAt){}
