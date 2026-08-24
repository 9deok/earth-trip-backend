package com.earthtrip.identity.adapter.out.persistence.session;

import java.util.Optional;

interface AuthSessionQuerydslSupport {

    Optional<AuthSessionAuthenticationRow> findAuthenticationByAccessTokenHash(String tokenHash);
}
