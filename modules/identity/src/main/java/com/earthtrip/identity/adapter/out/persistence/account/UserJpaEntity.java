package com.earthtrip.identity.adapter.out.persistence.account;

import com.earthtrip.identity.domain.EmailAddress;
import com.earthtrip.identity.domain.UserAccount;
import com.earthtrip.identity.domain.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "users")
class UserJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "email", nullable = false, length = 320, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected UserJpaEntity() { }

    private UserJpaEntity(UserAccount account) {
        id = account.id().toString();
        apply(account);
    }

    static UserJpaEntity from(UserAccount account) {
        return new UserJpaEntity(account);
    }

    void apply(UserAccount account) {
        email = account.email().value();
        passwordHash = account.passwordHash();
        displayName = account.displayName();
        status = account.status().name();
        emailVerifiedAt = account.emailVerifiedAt();
        createdAt = account.createdAt();
        updatedAt = account.updatedAt();
    }

    UserAccount toDomain() {
        return UserAccount.restore(
            UserId.from(id),
            new EmailAddress(email),
            passwordHash,
            displayName,
            UserAccount.Status.valueOf(status),
            emailVerifiedAt,
            createdAt,
            updatedAt
        );
    }
}
