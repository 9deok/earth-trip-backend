package com.earthtrip.identity.application.service.account;

import com.earthtrip.identity.application.port.in.CurrentAccountUseCase;
import com.earthtrip.identity.application.port.out.AccountDeletionStorePort;
import com.earthtrip.identity.application.port.out.UserAccountStorePort;
import com.earthtrip.identity.domain.UserAccount;
import com.earthtrip.identity.domain.UserId;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class CurrentAccountService implements CurrentAccountUseCase {

    private static final Duration DELETION_GRACE_PERIOD = Duration.ofDays(14);

    private final UserAccountStorePort accountStore;
    private final AccountDeletionStorePort deletionStore;
    private final Clock clock;

    CurrentAccountService(
        UserAccountStorePort accountStore,
        AccountDeletionStorePort deletionStore,
        Clock clock
    ) {
        this.accountStore = accountStore;
        this.deletionStore = deletionStore;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResult get(UUID userId) {
        return result(load(userId));
    }

    @Override
    public AccountResult updateName(UUID userId, String displayName) {
        UserAccount account = load(userId);
        account.rename(displayName, clock.instant());
        return result(accountStore.save(account));
    }

    @Override
    public DeletionResult requestDeletion(UUID userId) {
        UserAccount account = load(userId);
        Instant now = clock.instant();
        Instant scheduledAt = now.plus(DELETION_GRACE_PERIOD);
        AccountDeletionStorePort.DeletionRecord record = deletionStore.createOrGet(
            account.id(),
            now,
            scheduledAt
        );
        account.markDeletionPending(now);
        accountStore.save(account);
        return new DeletionResult(
            record.id(),
            record.requestedAt(),
            record.scheduledAt(),
            record.status()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DeletionResult currentDeletion(UUID userId) {
        UserAccount account = load(userId);
        AccountDeletionStorePort.DeletionRecord record = deletionStore.findPending(account.id())
            .orElseThrow(() -> EarthTripException.notFound(
                "DELETION_REQUEST_NOT_FOUND",
                "진행 중인 계정 삭제 요청이 없습니다."
            ));
        return new DeletionResult(
            record.id(),
            record.requestedAt(),
            record.scheduledAt(),
            record.status()
        );
    }

    @Override
    public void cancelDeletion(UUID userId) {
        UserAccount account = load(userId);
        if (deletionStore.findPending(account.id()).isEmpty()) {
            throw EarthTripException.notFound(
                "DELETION_REQUEST_NOT_FOUND",
                "진행 중인 계정 삭제 요청이 없습니다."
            );
        }
        Instant now = clock.instant();
        deletionStore.cancel(account.id(), now);
        account.cancelDeletion(now);
        accountStore.save(account);
    }

    private UserAccount load(UUID userId) {
        return accountStore.findById(new UserId(userId))
            .orElseThrow(() -> EarthTripException.notFound("ACCOUNT_NOT_FOUND", "계정을 찾을 수 없습니다."));
    }

    private static AccountResult result(UserAccount account) {
        return new AccountResult(
            account.id().value(),
            account.email().value(),
            account.displayName(),
            account.status().name(),
            account.emailVerifiedAt(),
            account.createdAt(),
            account.updatedAt()
        );
    }
}
