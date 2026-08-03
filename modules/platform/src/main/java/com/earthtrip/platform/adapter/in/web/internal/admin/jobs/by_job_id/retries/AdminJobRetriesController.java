package com.earthtrip.platform.adapter.in.web.internal.admin.jobs.by_job_id.retries;

import com.earthtrip.platform.application.port.in.InternalOperationsUseCase;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin/jobs/{jobId}/retries")
class AdminJobRetriesController {

    private final InternalOperationsUseCase useCase;

    AdminJobRetriesController(InternalOperationsUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    InternalOperationsUseCase.JobResult post(@PathVariable UUID jobId) {
        return useCase.retryJob(jobId);
    }
}
