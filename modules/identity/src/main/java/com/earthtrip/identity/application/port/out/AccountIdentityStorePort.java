package com.earthtrip.identity.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountIdentityStorePort {

    List<IdentityRecord> findByUser(UUID userId);

    Optional<IdentityRecord> findIdentity(UUID identityId);

    Optional<IdentityRecord> findIdentity(String provider, String subject);

    IdentityRecord saveIdentity(IdentityRecord identity);

    void deleteIdentity(UUID identityId);

    Optional<EmailChangeRecord> findEmailChangeByTokenHash(String tokenHash);

    Optional<EmailChangeRecord> findPendingEmailChange(UUID userId);

    EmailChangeRecord saveEmailChange(EmailChangeRecord request);

    record IdentityRecord(
        UUID id,
        UUID userId,
        String provider,
        String providerSubject,
        String providerEmail,
        Instant createdAt,
        Instant lastUsedAt,
        long version
    ) { }

    record EmailChangeRecord(
        UUID id,
        UUID userId,
        String newEmail,
        String tokenHash,
        String status,
        Instant expiresAt,
        Instant createdAt,
        Instant confirmedAt
    ) { }
}
