package com.earthtrip.planning.adapter.out.persistence.resource;

import java.io.Serializable;
import java.util.Objects;

@SuppressWarnings("serial")
final class PlanningUserStateId implements Serializable {
    private String resourceId;
    private String userId;
    private String stateType;

    protected PlanningUserStateId() {}

    PlanningUserStateId(String r, String u, String s) {
        resourceId = r;
        userId = u;
        stateType = s;
    }

    @Override
    public boolean equals(Object o) {
        return this == o
                || (o instanceof PlanningUserStateId that
                        && Objects.equals(resourceId, that.resourceId)
                        && Objects.equals(userId, that.userId)
                        && Objects.equals(stateType, that.stateType));
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceId, userId, stateType);
    }
}
