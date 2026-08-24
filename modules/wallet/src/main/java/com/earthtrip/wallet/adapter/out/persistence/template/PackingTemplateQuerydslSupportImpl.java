package com.earthtrip.wallet.adapter.out.persistence.template;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class PackingTemplateQuerydslSupportImpl implements PackingTemplateQuerydslSupport {

    private final JPAQueryFactory queryFactory;

    PackingTemplateQuerydslSupportImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<PackingTemplateJpaEntity> findVisible(String userId) {
        QPackingTemplateJpaEntity template = QPackingTemplateJpaEntity.packingTemplateJpaEntity;
        return queryFactory
                .selectFrom(template)
                .where(
                        template.deletedAt.isNull(),
                        template.userId.eq(userId).or(template.visibility.eq("PUBLIC")))
                .orderBy(template.updatedAt.desc())
                .fetch();
    }
}
