package com.earthtrip.identity.adapter.out.persistence.membership;

import com.earthtrip.identity.application.port.out.TripMemberStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "trip_members")
class TripMemberJpaEntity {
    @Id @Column(name = "id", nullable = false, length = 36) private String id;
    @Column(name = "trip_id", nullable = false, length = 36) private String tripId;
    @Column(name = "user_id", nullable = false, length = 36) private String userId;
    @Column(name = "role", nullable = false, length = 20) private String role;
    @Column(name = "status", nullable = false, length = 20) private String status;
    @Column(name = "joined_at", nullable = false) private Instant joinedAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(name = "version", nullable = false) private long version;

    protected TripMemberJpaEntity() { }
    TripMemberJpaEntity(TripMemberStorePort.MemberRecord r) { id = r.id().toString(); apply(r); }
    void apply(TripMemberStorePort.MemberRecord r) {
        tripId = r.tripId().toString(); userId = r.userId().toString(); role = r.role(); status = r.status();
        joinedAt = r.joinedAt(); updatedAt = r.updatedAt();
    }
    TripMemberStorePort.MemberRecord toRecord() {
        return new TripMemberStorePort.MemberRecord(
            UUID.fromString(id), UUID.fromString(tripId), UUID.fromString(userId), role, status,
            joinedAt, updatedAt, version
        );
    }
}
