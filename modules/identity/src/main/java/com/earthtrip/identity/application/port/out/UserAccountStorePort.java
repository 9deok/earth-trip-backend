package com.earthtrip.identity.application.port.out;

import com.earthtrip.identity.domain.EmailAddress;
import com.earthtrip.identity.domain.UserAccount;
import com.earthtrip.identity.domain.UserId;
import java.util.Optional;

public interface UserAccountStorePort {

    Optional<UserAccount> findById(UserId userId);

    Optional<UserAccount> findByEmail(EmailAddress email);

    UserAccount save(UserAccount account);
}
