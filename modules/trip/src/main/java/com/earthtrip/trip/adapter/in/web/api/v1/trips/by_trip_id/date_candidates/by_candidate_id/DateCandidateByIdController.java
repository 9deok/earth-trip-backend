package com.earthtrip.trip.adapter.in.web.api.v1.trips.by_trip_id.date_candidates.by_candidate_id;
import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.DateCandidateUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/trips/{tripId}/date-candidates/{candidateId}") class DateCandidateByIdController{
    private final DateCandidateUseCase useCase;private final CurrentActor actor;
    DateCandidateByIdController(DateCandidateUseCase u,CurrentActor a){useCase=u;actor=a;}
    @PatchMapping DateCandidateResponse patch(@PathVariable UUID tripId,@PathVariable UUID candidateId,@Valid @RequestBody DateCandidateUpdateRequest r){return response(useCase.update(tripId,candidateId,actor.requireUserId(),new DateCandidateUseCase.CandidateCommand(candidateId,r.startDate(),r.endDate(),r.note(),r.status(),r.baseVersion())));}
    @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable UUID tripId,@PathVariable UUID candidateId,@Valid @RequestBody DateCandidateDeleteRequest r){useCase.delete(tripId,candidateId,actor.requireUserId(),r.baseVersion());}
    static DateCandidateResponse response(DateCandidateUseCase.CandidateResult d){return new DateCandidateResponse(d.candidateId(),d.tripId(),d.startDate(),d.endDate(),d.note(),d.status(),d.availability().stream().map(a->new AvailabilityResponse(a.userId(),a.availability(),a.note(),a.updatedAt())).toList(),d.version(),d.createdBy(),d.createdAt(),d.updatedAt());}
}
record DateCandidateUpdateRequest(LocalDate startDate,LocalDate endDate,String note,String status,@Min(0) long baseVersion){}
record DateCandidateDeleteRequest(@Min(0) long baseVersion){}
record AvailabilityResponse(UUID userId,String availability,String note,Instant updatedAt){}
record DateCandidateResponse(UUID candidateId,UUID tripId,LocalDate startDate,LocalDate endDate,String note,String status,List<AvailabilityResponse> availability,long version,UUID createdBy,Instant createdAt,Instant updatedAt){}
