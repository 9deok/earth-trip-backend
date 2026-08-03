package com.earthtrip.identity.adapter.out.persistence.linkedidentity;

import com.earthtrip.identity.application.port.out.AccountIdentityStorePort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class AccountIdentityPersistenceAdapter implements AccountIdentityStorePort {
    private final LinkedIdentityJpaRepository identities;
    private final EmailChangeJpaRepository emailChanges;
    AccountIdentityPersistenceAdapter(
        LinkedIdentityJpaRepository identities, EmailChangeJpaRepository emailChanges
    ) { this.identities = identities; this.emailChanges = emailChanges; }
    @Override public List<IdentityRecord> findByUser(UUID userId) {
        return identities.findAllByUserIdOrderByCreatedAtAsc(userId.toString()).stream()
            .map(LinkedIdentityJpaEntity::toRecord).toList();
    }
    @Override public Optional<IdentityRecord> findIdentity(UUID id) {
        return identities.findById(id.toString()).map(LinkedIdentityJpaEntity::toRecord);
    }
    @Override public Optional<IdentityRecord> findIdentity(String provider,String subject) {
        return identities.findByProviderAndProviderSubject(provider,subject)
            .map(LinkedIdentityJpaEntity::toRecord);
    }
    @Override public IdentityRecord saveIdentity(IdentityRecord record) {
        LinkedIdentityJpaEntity entity=identities.findById(record.id().toString())
            .map(existing->{existing.apply(record);return existing;})
            .orElseGet(()->new LinkedIdentityJpaEntity(record));
        return identities.saveAndFlush(entity).toRecord();
    }
    @Override public void deleteIdentity(UUID id){identities.deleteById(id.toString());}
    @Override public Optional<EmailChangeRecord> findEmailChangeByTokenHash(String hash){
        return emailChanges.findByTokenHash(hash).map(EmailChangeJpaEntity::toRecord);
    }
    @Override public Optional<EmailChangeRecord> findPendingEmailChange(UUID userId){
        return emailChanges.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId.toString(),"PENDING")
            .map(EmailChangeJpaEntity::toRecord);
    }
    @Override public EmailChangeRecord saveEmailChange(EmailChangeRecord record){
        EmailChangeJpaEntity entity=emailChanges.findById(record.id().toString())
            .map(existing->{existing.apply(record);return existing;})
            .orElseGet(()->new EmailChangeJpaEntity(record));
        return emailChanges.saveAndFlush(entity).toRecord();
    }
}
