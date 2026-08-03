package com.earthtrip.trip.adapter.out.persistence.destination;

import java.io.Serializable;
import java.util.Objects;

@SuppressWarnings("serial")
final class DestinationPreferenceId implements Serializable {
    private String candidateId; private String userId;
    protected DestinationPreferenceId() { }
    DestinationPreferenceId(String candidateId, String userId) {
        this.candidateId = candidateId; this.userId = userId;
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DestinationPreferenceId that)) return false;
        return Objects.equals(candidateId, that.candidateId) && Objects.equals(userId, that.userId);
    }
    @Override public int hashCode() { return Objects.hash(candidateId, userId); }
}
