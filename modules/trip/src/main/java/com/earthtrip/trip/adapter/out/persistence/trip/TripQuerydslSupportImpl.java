package com.earthtrip.trip.adapter.out.persistence.trip;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class TripQuerydslSupportImpl implements TripQuerydslSupport {

    private final JPAQueryFactory queryFactory;

    TripQuerydslSupportImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Optional<TripJpaEntity> findById(String tripId) {
        QTripJpaEntity trip = QTripJpaEntity.tripJpaEntity;

        return Optional.ofNullable(
            queryFactory
                .selectFrom(trip)
                .where(trip.id.eq(tripId))
                .fetchOne()
        );
    }
}
