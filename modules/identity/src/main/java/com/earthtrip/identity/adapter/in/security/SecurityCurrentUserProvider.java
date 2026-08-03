package com.earthtrip.identity.adapter.in.security;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
class SecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Optional<UUID> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof EarthTripPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal.userId());
    }

    @Override
    public Optional<UUID> currentSessionId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof EarthTripPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal.sessionId());
    }
}
