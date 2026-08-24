package com.earthtrip.notification.adapter.out.persistence.notification;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class NotificationQuerydslSupportImpl implements NotificationQuerydslSupport {

    private final JPAQueryFactory queryFactory;

    NotificationQuerydslSupportImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<GroupCount> unreadByTrip(String userId) {
        QNotificationJpaEntity notification = QNotificationJpaEntity.notificationJpaEntity;
        NumberExpression<Long> total = notification.count();
        return queryFactory
                .select(notification.tripId, total)
                .from(notification)
                .where(
                        notification.userId.eq(userId),
                        notification.hiddenAt.isNull(),
                        notification.readAt.isNull(),
                        notification.tripId.isNotNull())
                .groupBy(notification.tripId)
                .fetch()
                .stream()
                .map(row -> group(row, notification.tripId, total))
                .toList();
    }

    @Override
    public List<GroupCount> unreadByType(String userId) {
        QNotificationJpaEntity notification = QNotificationJpaEntity.notificationJpaEntity;
        NumberExpression<Long> total = notification.count();
        return queryFactory
                .select(notification.type, total)
                .from(notification)
                .where(
                        notification.userId.eq(userId),
                        notification.hiddenAt.isNull(),
                        notification.readAt.isNull())
                .groupBy(notification.type)
                .fetch()
                .stream()
                .map(row -> group(row, notification.type, total))
                .toList();
    }

    private static GroupCount group(
            Tuple row,
            com.querydsl.core.types.Expression<String> key,
            NumberExpression<Long> total) {
        Long count = row.get(total);
        return new GroupCount(row.get(key), count == null ? 0 : count);
    }
}
