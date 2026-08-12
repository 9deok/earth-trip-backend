package com.earthtrip.identity.application.port.out;

import com.earthtrip.identity.domain.EmailAddress;
import com.earthtrip.identity.domain.UserAccount;
import com.earthtrip.identity.domain.UserId;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public interface UserAccountStorePort {

    Optional<UserAccount> findById(UserId userId);

    Optional<UserAccount> findByEmail(EmailAddress email);

    default Map<UserId, UserAccount> findAllByIds(Collection<UserId> userIds) {
        Map<UserId, UserAccount> result = new LinkedHashMap<>();
        for (UserId userId : userIds) findById(userId).ifPresent(user -> result.put(userId, user));
        return result;
    }

    UserAccount save(UserAccount account);
}
