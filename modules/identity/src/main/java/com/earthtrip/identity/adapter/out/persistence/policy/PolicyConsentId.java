package com.earthtrip.identity.adapter.out.persistence.policy;

import java.io.Serializable;
import java.util.Objects;

@SuppressWarnings("serial")
final class PolicyConsentId implements Serializable {

    private String userId;
    private String policyId;

    protected PolicyConsentId() {}

    PolicyConsentId(String userId, String policyId) {
        this.userId = userId;
        this.policyId = policyId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PolicyConsentId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(policyId, that.policyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, policyId);
    }
}
