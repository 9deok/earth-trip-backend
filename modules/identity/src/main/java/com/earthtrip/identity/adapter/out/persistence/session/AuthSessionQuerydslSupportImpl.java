package com.earthtrip.identity.adapter.out.persistence.session;

import com.earthtrip.identity.adapter.out.persistence.account.QUserJpaEntity;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class AuthSessionQuerydslSupportImpl implements AuthSessionQuerydslSupport {

    private final JPAQueryFactory queryFactory;

    AuthSessionQuerydslSupportImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Optional<AuthSessionAuthenticationRow> findAuthenticationByAccessTokenHash(
            String tokenHash) {
        QAuthSessionJpaEntity session = QAuthSessionJpaEntity.authSessionJpaEntity;
        QUserJpaEntity account = QUserJpaEntity.userJpaEntity;
        Tuple row =
                queryFactory
                        .select(
                                session.id,
                                session.userId,
                                session.accessTokenHash,
                                session.refreshTokenHash,
                                session.deviceName,
                                session.accessExpiresAt,
                                session.refreshExpiresAt,
                                session.lastUsedAt,
                                session.revokedAt,
                                session.createdAt,
                                account.displayName,
                                account.status)
                        .from(session)
                        .join(account)
                        .on(session.userId.eq(account.id))
                        .where(session.accessTokenHash.eq(tokenHash))
                        .fetchOne();
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(
                new AuthSessionAuthenticationRow(
                        row.get(session.id),
                        row.get(session.userId),
                        row.get(session.accessTokenHash),
                        row.get(session.refreshTokenHash),
                        row.get(session.deviceName),
                        row.get(session.accessExpiresAt),
                        row.get(session.refreshExpiresAt),
                        row.get(session.lastUsedAt),
                        row.get(session.revokedAt),
                        row.get(session.createdAt),
                        row.get(account.displayName),
                        row.get(account.status)));
    }
}
