package com.earthtrip.platform.adapter.out.persistence.share;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
class PublicTripReactionJpaId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "publication_id", nullable = false, length = 36)
    private String publicationId;

    @Column(name = "actor_user_id", nullable = false, length = 36)
    private String actorUserId;

    @Column(name = "reaction_type", nullable = false, length = 20)
    private String reactionType;

    protected PublicTripReactionJpaId() {}

    PublicTripReactionJpaId(String publicationId, String actorUserId, String reactionType) {
        this.publicationId = publicationId;
        this.actorUserId = actorUserId;
        this.reactionType = reactionType;
    }

    String publicationId() {
        return publicationId;
    }

    String actorUserId() {
        return actorUserId;
    }

    String reactionType() {
        return reactionType;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PublicTripReactionJpaId that)) {
            return false;
        }
        return Objects.equals(publicationId, that.publicationId)
                && Objects.equals(actorUserId, that.actorUserId)
                && Objects.equals(reactionType, that.reactionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicationId, actorUserId, reactionType);
    }
}
