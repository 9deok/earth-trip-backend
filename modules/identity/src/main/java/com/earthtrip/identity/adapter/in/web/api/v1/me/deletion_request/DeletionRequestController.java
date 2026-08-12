package com.earthtrip.identity.adapter.in.web.api.v1.me.deletion_request;

import com.earthtrip.identity.application.port.in.CurrentAccountUseCase;
import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/deletion-request")
class DeletionRequestController {

    private final CurrentAccountUseCase useCase;
    private final CurrentUserProvider currentUser;

    DeletionRequestController(CurrentAccountUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @GetMapping
    DeletionRequestResponse get() {
        return response(useCase.currentDeletion(currentUser.requireUserId()));
    }

    @PostMapping
    ResponseEntity<DeletionRequestResponse> request() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(useCase.requestDeletion(currentUser.requireUserId())));
    }

    @DeleteMapping
    ResponseEntity<Void> cancel() {
        useCase.cancelDeletion(currentUser.requireUserId());
        return ResponseEntity.noContent().build();
    }

    private static DeletionRequestResponse response(CurrentAccountUseCase.DeletionResult result) {
        return new DeletionRequestResponse(
                result.requestId(),
                result.requestedAt(),
                result.scheduledDeletionAt(),
                result.status());
    }
}

record DeletionRequestResponse(
        UUID requestId, Instant requestedAt, Instant scheduledDeletionAt, String status) {}
