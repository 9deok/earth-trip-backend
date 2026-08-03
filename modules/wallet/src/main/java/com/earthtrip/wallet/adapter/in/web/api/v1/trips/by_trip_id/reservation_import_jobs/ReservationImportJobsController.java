package com.earthtrip.wallet.adapter.in.web.api.v1.trips.by_trip_id.reservation_import_jobs;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.ReservationImportUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/reservation-import-jobs")
class ReservationImportJobsController {

    private final ReservationImportUseCase useCase;
    private final CurrentActor actor;

    ReservationImportJobsController(ReservationImportUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    ReservationImportUseCase.ImportResult create(
        @PathVariable UUID tripId,
        @Valid @RequestBody ReservationImportJobRequest request
    ) {
        return useCase.create(
            tripId,
            actor.requireUserId(),
            new ReservationImportUseCase.ImportCommand(
                request.requestId(), request.sourceType(), request.sourcePayload(),
                request.candidates() == null
                    ? List.of()
                    : request.candidates().stream().map(ReservationImportCandidateRequest::command)
                        .toList()
            )
        );
    }
}

record ReservationImportJobRequest(
    @NotNull UUID requestId,
    @NotBlank String sourceType,
    @NotNull Map<String, Object> sourcePayload,
    @Size(max = 100) List<@Valid ReservationImportCandidateRequest> candidates
) { }

record ReservationImportCandidateRequest(
    @NotNull UUID candidateId,
    @NotBlank @Size(max = 200) String title,
    String candidateType,
    @NotNull Map<String, Object> payload,
    @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal confidence
) {
    ReservationImportUseCase.CandidateCommand command() {
        return new ReservationImportUseCase.CandidateCommand(
            candidateId, title, candidateType, payload, confidence
        );
    }
}
