package com.earthtrip.identity.adapter.in.web.api.v1.invitations.by_token;

import com.earthtrip.identity.application.port.in.InvitationUseCase;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invitations/{token}")
class InvitationPreviewController {
    private final InvitationUseCase useCase;

    InvitationPreviewController(InvitationUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    InvitationPreviewResponse get(@PathVariable String token) {
        InvitationUseCase.PreviewResult p = useCase.preview(token);
        return new InvitationPreviewResponse(
                p.invitationId(),
                p.tripId(),
                p.tripTitle(),
                p.startDate(),
                p.endDate(),
                p.invitedEmail(),
                p.role(),
                p.status(),
                p.expiresAt());
    }
}

record InvitationPreviewResponse(
        UUID invitationId,
        UUID tripId,
        String tripTitle,
        LocalDate startDate,
        LocalDate endDate,
        String invitedEmail,
        String role,
        String status,
        Instant expiresAt) {}
