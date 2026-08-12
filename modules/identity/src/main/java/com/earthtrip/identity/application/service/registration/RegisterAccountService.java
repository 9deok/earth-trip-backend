package com.earthtrip.identity.application.service.registration;

import com.earthtrip.identity.application.port.in.RegisterAccountUseCase;
import com.earthtrip.identity.application.port.out.CredentialPort;
import com.earthtrip.identity.application.port.out.UserAccountStorePort;
import com.earthtrip.identity.domain.EmailAddress;
import com.earthtrip.identity.domain.UserAccount;
import com.earthtrip.identity.domain.UserId;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class RegisterAccountService implements RegisterAccountUseCase {

    private final UserAccountStorePort accountStore;
    private final CredentialPort credentialPort;
    private final Clock clock;

    RegisterAccountService(
            UserAccountStorePort accountStore, CredentialPort credentialPort, Clock clock) {
        this.accountStore = accountStore;
        this.credentialPort = credentialPort;
        this.clock = clock;
    }

    @Override
    public Result register(Command command) {
        Objects.requireNonNull(command.requestId(), "요청 ID는 필수입니다.");
        validatePassword(command.password());
        EmailAddress email = new EmailAddress(command.email());

        UserAccount sameRequest =
                accountStore.findById(new UserId(command.requestId())).orElse(null);
        if (sameRequest != null) {
            if (!sameRequest.email().equals(email)) {
                throw EarthTripException.conflict(
                        "IDEMPOTENCY_KEY_REUSED", "같은 요청 ID가 다른 계정 생성 요청에 사용됐습니다.");
            }
            return result(sameRequest);
        }
        if (accountStore.findByEmail(email).isPresent()) {
            throw EarthTripException.conflict("EMAIL_ALREADY_REGISTERED", "이미 가입된 이메일입니다.");
        }

        Instant now = clock.instant();
        UserAccount account =
                UserAccount.register(
                        new UserId(command.requestId()),
                        email,
                        credentialPort.hashPassword(command.password()),
                        command.displayName(),
                        now);
        return result(accountStore.save(account));
    }

    private static Result result(UserAccount account) {
        return new Result(
                account.id().value(),
                account.email().value(),
                account.displayName(),
                account.status().name(),
                account.createdAt());
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 10 || password.length() > 128) {
            throw EarthTripException.badRequest("WEAK_PASSWORD", "비밀번호는 10자 이상 128자 이하로 입력해 주세요.");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw EarthTripException.badRequest("WEAK_PASSWORD", "비밀번호에는 문자와 숫자가 모두 필요합니다.");
        }
    }
}
