package com.earthtrip.planning.adapter.out.persistence.link;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
class CandidateSourceLinkId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "candidate_id", nullable = false, length = 36)
    private String candidateId;

    @Column(name = "source_id", nullable = false, length = 36)
    private String sourceId;

    protected CandidateSourceLinkId() {}

    CandidateSourceLinkId(String candidateId, String sourceId) {
        this.candidateId = candidateId;
        this.sourceId = sourceId;
    }

    String candidateId() {
        return candidateId;
    }

    String sourceId() {
        return sourceId;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof CandidateSourceLinkId that
                        && Objects.equals(candidateId, that.candidateId)
                        && Objects.equals(sourceId, that.sourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(candidateId, sourceId);
    }
}
