package com.earthtrip.platform.adapter.in.web.internal.admin.audit_events;

import com.earthtrip.platform.application.port.in.InternalOperationsUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin/audit-events")
class AdminAuditEventsController {

    private final InternalOperationsUseCase useCase;

    AdminAuditEventsController(InternalOperationsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    List<InternalOperationsUseCase.AuditResult> get(
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String targetType,
        @RequestParam(required = false) String targetId,
        @RequestParam(defaultValue = "50") int limit
    ) {
        return useCase.auditEvents(action, targetType, targetId, limit);
    }
}
