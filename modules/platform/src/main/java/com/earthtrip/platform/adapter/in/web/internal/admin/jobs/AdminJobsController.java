package com.earthtrip.platform.adapter.in.web.internal.admin.jobs;

import com.earthtrip.platform.application.port.in.InternalOperationsUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin/jobs")
class AdminJobsController {

    private final InternalOperationsUseCase useCase;

    AdminJobsController(InternalOperationsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    List<InternalOperationsUseCase.JobResult> get(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String jobType,
            @RequestParam(defaultValue = "50") int limit) {
        return useCase.jobs(status, jobType, limit);
    }
}
