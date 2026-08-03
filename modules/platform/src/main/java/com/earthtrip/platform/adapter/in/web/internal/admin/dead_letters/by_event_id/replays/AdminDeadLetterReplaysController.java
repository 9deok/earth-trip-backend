package com.earthtrip.platform.adapter.in.web.internal.admin.dead_letters.by_event_id.replays;

import com.earthtrip.platform.application.port.in.InternalOperationsUseCase;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin/dead-letters/{eventId}/replays")
class AdminDeadLetterReplaysController {

    private final InternalOperationsUseCase useCase;

    AdminDeadLetterReplaysController(InternalOperationsUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    InternalOperationsUseCase.JobResult post(@PathVariable UUID eventId) {
        return useCase.replayDeadLetter(eventId);
    }
}
