package com.earthtrip.wallet.adapter.out.persistence.template;

import com.earthtrip.wallet.application.port.out.PackingTemplateStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "preparation_suggestion_dismissals")
@IdClass(PreparationSuggestionDismissalId.class)
class PreparationSuggestionDismissalJpaEntity {

    @Id
    @Column(name = "suggestion_id", nullable = false, length = 36)
    private String suggestionId;

    @Id
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "dismissed_at", nullable = false)
    private Instant dismissedAt;

    protected PreparationSuggestionDismissalJpaEntity() { }

    PreparationSuggestionDismissalJpaEntity(
        PackingTemplateStorePort.DismissalRecord record
    ) {
        suggestionId = record.suggestionId().toString();
        userId = record.userId().toString();
        tripId = record.tripId().toString();
        reason = record.reason();
        dismissedAt = record.dismissedAt();
    }
}
