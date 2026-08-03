package com.earthtrip.platform.adapter.in.web.internal.admin.dead_letters;

import com.earthtrip.platform.application.port.in.InternalOperationsUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin/dead-letters")
class AdminDeadLettersController {

    private final InternalOperationsUseCase useCase;

    AdminDeadLettersController(InternalOperationsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    List<InternalOperationsUseCase.DeadLetterResult> get(
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "50") int limit
    ) {
        return useCase.deadLetters(status, limit);
    }
}
