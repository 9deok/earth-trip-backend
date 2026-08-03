package com.earthtrip.identity.adapter.in.security;

import java.security.Principal;
import java.util.UUID;

record EarthTripPrincipal(UUID userId, UUID sessionId, String displayName) implements Principal {

    @Override
    public String getName() {
        return userId.toString();
    }
}
