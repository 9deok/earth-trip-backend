package com.earthtrip.platform.adapter.out.persistence.operation;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class OperationalQuerydslSupportImpl implements OperationalQuerydslSupport {

    private final JPAQueryFactory queryFactory;

    OperationalQuerydslSupportImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<OperationalJobJpaEntity> searchJobs(String status, String jobType, int limit) {
        QOperationalJobJpaEntity job = QOperationalJobJpaEntity.operationalJobJpaEntity;
        BooleanBuilder conditions = new BooleanBuilder();
        if (status != null) {
            conditions.and(job.status.eq(status));
        }
        if (jobType != null) {
            conditions.and(job.jobType.eq(jobType));
        }
        return queryFactory
                .selectFrom(job)
                .where(conditions)
                .orderBy(job.createdAt.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<DeadLetterJpaEntity> searchDeadLetters(String status, int limit) {
        QDeadLetterJpaEntity deadLetter = QDeadLetterJpaEntity.deadLetterJpaEntity;
        BooleanBuilder conditions = new BooleanBuilder();
        if (status != null) {
            conditions.and(deadLetter.status.eq(status));
        }
        return queryFactory
                .selectFrom(deadLetter)
                .where(conditions)
                .orderBy(deadLetter.createdAt.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<AdminAuditJpaEntity> searchAudits(
            String action, String targetType, String targetId, int limit) {
        QAdminAuditJpaEntity audit = QAdminAuditJpaEntity.adminAuditJpaEntity;
        BooleanBuilder conditions = new BooleanBuilder();
        if (action != null) {
            conditions.and(audit.action.eq(action));
        }
        if (targetType != null) {
            conditions.and(audit.targetType.eq(targetType));
        }
        if (targetId != null) {
            conditions.and(audit.targetId.eq(targetId));
        }
        return queryFactory
                .selectFrom(audit)
                .where(conditions)
                .orderBy(audit.occurredAt.desc())
                .limit(limit)
                .fetch();
    }
}
