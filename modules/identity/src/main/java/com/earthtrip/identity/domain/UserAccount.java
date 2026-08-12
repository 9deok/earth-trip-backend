package com.earthtrip.identity.domain;

import java.time.Instant;
import java.util.Objects;

public final class UserAccount {

    public enum Status {
        PENDING_VERIFICATION,
        ACTIVE,
        DELETION_PENDING,
        DELETED
    }

    private final UserId id;
    private EmailAddress email;
    private String passwordHash;
    private String displayName;
    private Status status;
    private Instant emailVerifiedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    private UserAccount(
            UserId id,
            EmailAddress email,
            String passwordHash,
            String displayName,
            Status status,
            Instant emailVerifiedAt,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = requireText(passwordHash, "비밀번호 해시는 필수입니다.", 255);
        this.displayName = requireText(displayName, "표시 이름은 필수입니다.", 80);
        this.status = Objects.requireNonNull(status);
        this.emailVerifiedAt = emailVerifiedAt;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static UserAccount register(
            UserId id, EmailAddress email, String passwordHash, String displayName, Instant now) {
        return new UserAccount(
                id, email, passwordHash, displayName, Status.PENDING_VERIFICATION, null, now, now);
    }

    public static UserAccount restore(
            UserId id,
            EmailAddress email,
            String passwordHash,
            String displayName,
            Status status,
            Instant emailVerifiedAt,
            Instant createdAt,
            Instant updatedAt) {
        return new UserAccount(
                id,
                email,
                passwordHash,
                displayName,
                status,
                emailVerifiedAt,
                createdAt,
                updatedAt);
    }

    public void verifyEmail(Instant now) {
        if (status == Status.DELETED) {
            throw new IllegalStateException("삭제된 계정은 인증할 수 없습니다.");
        }
        emailVerifiedAt = Objects.requireNonNull(now);
        status = Status.ACTIVE;
        updatedAt = now;
    }

    public void rename(String newDisplayName, Instant now) {
        displayName = requireText(newDisplayName, "표시 이름은 필수입니다.", 80);
        updatedAt = Objects.requireNonNull(now);
    }

    public void markDeletionPending(Instant now) {
        if (status == Status.DELETED) {
            throw new IllegalStateException("이미 삭제된 계정입니다.");
        }
        status = Status.DELETION_PENDING;
        updatedAt = Objects.requireNonNull(now);
    }

    public void cancelDeletion(Instant now) {
        if (status == Status.DELETION_PENDING) {
            status = emailVerifiedAt == null ? Status.PENDING_VERIFICATION : Status.ACTIVE;
            updatedAt = Objects.requireNonNull(now);
        }
    }

    public void changePassword(String newPasswordHash, Instant now) {
        if (status == Status.DELETED) {
            throw new IllegalStateException("삭제된 계정의 비밀번호는 변경할 수 없습니다.");
        }
        passwordHash = requireText(newPasswordHash, "비밀번호 해시는 필수입니다.", 255);
        updatedAt = Objects.requireNonNull(now);
    }

    public void changeEmail(EmailAddress newEmail, Instant now) {
        if (status == Status.DELETED) {
            throw new IllegalStateException("삭제된 계정의 이메일은 변경할 수 없습니다.");
        }
        email = Objects.requireNonNull(newEmail);
        emailVerifiedAt = Objects.requireNonNull(now);
        updatedAt = now;
    }

    public boolean canSignIn() {
        return status == Status.ACTIVE || status == Status.DELETION_PENDING;
    }

    public UserId id() {
        return id;
    }

    public EmailAddress email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String displayName() {
        return displayName;
    }

    public Status status() {
        return status;
    }

    public Instant emailVerifiedAt() {
        return emailVerifiedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static String requireText(String value, String message, int maxLength) {
        Objects.requireNonNull(value, message);
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }
}
