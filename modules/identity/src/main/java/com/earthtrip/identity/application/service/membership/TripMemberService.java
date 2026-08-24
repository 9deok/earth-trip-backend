package com.earthtrip.identity.application.service.membership;

import com.earthtrip.identity.application.port.in.TripMemberUseCase;
import com.earthtrip.identity.application.port.out.OwnershipTransferStorePort;
import com.earthtrip.identity.application.port.out.TripMemberStorePort;
import com.earthtrip.identity.application.port.out.UserAccountStorePort;
import com.earthtrip.identity.domain.UserAccount;
import com.earthtrip.identity.domain.UserId;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.spi.TripChangePublisher;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class TripMemberService implements TripMemberUseCase {
    private final TripAccess tripAccess;
    private final TripMemberStorePort members;
    private final UserAccountStorePort users;
    private final OwnershipTransferStorePort transfers;
    private final TripChangePublisher changes;
    private final Clock clock;

    TripMemberService(
            TripAccess tripAccess,
            TripMemberStorePort members,
            UserAccountStorePort users,
            OwnershipTransferStorePort transfers,
            TripChangePublisher changes,
            Clock clock) {
        this.tripAccess = tripAccess;
        this.members = members;
        this.users = users;
        this.transfers = transfers;
        this.changes = changes;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResult> list(UUID tripId, UUID actorUserId) {
        TripAccess.AccessResult access = tripAccess.requireViewer(tripId, actorUserId);
        List<TripMemberStorePort.MemberRecord> activeMembers =
                members.findAll(tripId).stream()
                        .filter(member -> member.status().equals("ACTIVE"))
                        .toList();
        LinkedHashSet<UserId> userIds = new LinkedHashSet<>();
        userIds.add(new UserId(access.ownerUserId()));
        activeMembers.forEach(member -> userIds.add(new UserId(member.userId())));
        Map<UserId, UserAccount> accounts = users.findAllByIds(userIds);
        List<MemberResult> result = new ArrayList<>();
        UserAccount owner = loadUser(accounts, access.ownerUserId());
        result.add(
                new MemberResult(
                        syntheticOwnerMemberId(tripId, owner.id().value()),
                        owner.id().value(),
                        owner.displayName(),
                        owner.email().value(),
                        "OWNER",
                        "ACTIVE",
                        owner.id().value().equals(actorUserId),
                        owner.createdAt(),
                        access.tripVersion()));
        activeMembers.stream()
                .forEach(
                        member -> {
                            UserAccount user = loadUser(accounts, member.userId());
                            result.add(
                                    new MemberResult(
                                            member.id(),
                                            member.userId(),
                                            user.displayName(),
                                            user.email().value(),
                                            member.role(),
                                            member.status(),
                                            member.userId().equals(actorUserId),
                                            member.joinedAt(),
                                            member.version()));
                        });
        return List.copyOf(result);
    }

    @Override
    public MemberResult changeRole(
            UUID tripId, UUID memberId, UUID actorUserId, String rawRole, long baseVersion) {
        tripAccess.requireOwner(tripId, actorUserId);
        TripMemberStorePort.MemberRecord member = loadMember(tripId, memberId);
        verifyVersion(member, baseVersion);
        String role = role(rawRole);
        if (role.equals("OWNER"))
            throw EarthTripException.badRequest(
                    "USE_OWNERSHIP_TRANSFER", "소유자 변경은 소유권 이전 절차를 사용해 주세요.");
        TripMemberStorePort.MemberRecord saved =
                members.save(
                        new TripMemberStorePort.MemberRecord(
                                member.id(),
                                member.tripId(),
                                member.userId(),
                                role,
                                member.status(),
                                member.joinedAt(),
                                clock.instant(),
                                member.version()));
        changes.publish(tripId, actorUserId, "ROLE_CHANGED", "TRIP_MEMBER", memberId);
        return result(saved, actorUserId);
    }

    @Override
    public void remove(UUID tripId, UUID memberId, UUID actorUserId, long baseVersion) {
        tripAccess.requireOwner(tripId, actorUserId);
        TripMemberStorePort.MemberRecord member = loadMember(tripId, memberId);
        verifyVersion(member, baseVersion);
        members.delete(memberId);
        changes.publish(tripId, actorUserId, "REMOVED", "TRIP_MEMBER", memberId);
    }

    @Override
    public void leave(UUID tripId, UUID actorUserId) {
        TripAccess.AccessResult access = tripAccess.requireViewer(tripId, actorUserId);
        if (access.ownerUserId().equals(actorUserId))
            throw EarthTripException.conflict(
                    "OWNER_MUST_TRANSFER_OWNERSHIP", "소유권을 이전한 뒤 여행에서 나갈 수 있습니다.");
        TripMemberStorePort.MemberRecord member =
                members.findByTripAndUser(tripId, actorUserId)
                        .orElseThrow(
                                () ->
                                        EarthTripException.notFound(
                                                "MEMBER_NOT_FOUND", "여행 멤버를 찾을 수 없습니다."));
        members.delete(member.id());
        changes.publish(tripId, actorUserId, "LEFT", "TRIP_MEMBER", member.id());
    }

    @Override
    public void transferOwnership(
            UUID tripId, UUID actorUserId, UUID toMemberId, boolean confirmed) {
        tripAccess.requireOwner(tripId, actorUserId);
        if (!confirmed)
            throw EarthTripException.badRequest(
                    "OWNERSHIP_TRANSFER_CONFIRMATION_REQUIRED", "소유권 이전 확인이 필요합니다.");
        TripMemberStorePort.MemberRecord target = loadMember(tripId, toMemberId);
        if (!target.status().equals("ACTIVE"))
            throw EarthTripException.conflict(
                    "TARGET_MEMBER_INACTIVE", "활성 멤버에게만 소유권을 이전할 수 있습니다.");
        Instant now = clock.instant();
        tripAccess.transferOwnership(tripId, actorUserId, target.userId());
        members.delete(target.id());
        TripMemberStorePort.MemberRecord formerOwner =
                members.findByTripAndUser(tripId, actorUserId)
                        .orElse(
                                new TripMemberStorePort.MemberRecord(
                                        UUID.randomUUID(),
                                        tripId,
                                        actorUserId,
                                        "EDITOR",
                                        "ACTIVE",
                                        now,
                                        now,
                                        0));
        members.save(
                new TripMemberStorePort.MemberRecord(
                        formerOwner.id(),
                        tripId,
                        actorUserId,
                        "EDITOR",
                        "ACTIVE",
                        formerOwner.joinedAt(),
                        now,
                        formerOwner.version()));
        transfers.record(UUID.randomUUID(), tripId, actorUserId, target.userId(), now);
        changes.publish(
                tripId,
                actorUserId,
                "OWNERSHIP_TRANSFERRED",
                "TRIP_MEMBER",
                target.id(),
                java.util.Map.of("newOwnerUserId", target.userId()));
    }

    private TripMemberStorePort.MemberRecord loadMember(UUID tripId, UUID memberId) {
        return members.findById(memberId)
                .filter(member -> member.tripId().equals(tripId))
                .orElseThrow(
                        () -> EarthTripException.notFound("MEMBER_NOT_FOUND", "여행 멤버를 찾을 수 없습니다."));
    }

    private UserAccount loadUser(UUID userId) {
        return users.findById(new UserId(userId))
                .orElseThrow(
                        () -> EarthTripException.notFound("ACCOUNT_NOT_FOUND", "계정을 찾을 수 없습니다."));
    }

    private static UserAccount loadUser(Map<UserId, UserAccount> accounts, UUID userId) {
        UserAccount user = accounts.get(new UserId(userId));
        if (user == null) throw EarthTripException.notFound("ACCOUNT_NOT_FOUND", "계정을 찾을 수 없습니다.");
        return user;
    }

    private MemberResult result(TripMemberStorePort.MemberRecord member, UUID currentUserId) {
        UserAccount user = loadUser(member.userId());
        return new MemberResult(
                member.id(),
                member.userId(),
                user.displayName(),
                user.email().value(),
                member.role(),
                member.status(),
                member.userId().equals(currentUserId),
                member.joinedAt(),
                member.version());
    }

    private static String role(String raw) {
        if (raw == null) throw EarthTripException.badRequest("ROLE_REQUIRED", "역할이 필요합니다.");
        String role = raw.strip().toUpperCase(Locale.ROOT);
        if (!role.equals("OWNER") && !role.equals("EDITOR") && !role.equals("VIEWER")) {
            throw EarthTripException.badRequest("INVALID_ROLE", "지원하지 않는 역할입니다.");
        }
        return role;
    }

    private static void verifyVersion(TripMemberStorePort.MemberRecord member, long version) {
        if (member.version() != version)
            throw new EarthTripException(
                    "VERSION_CONFLICT",
                    409,
                    "다른 권한 변경이 먼저 저장되었습니다.",
                    java.util.Map.of("serverVersion", member.version()));
    }

    private static UUID syntheticOwnerMemberId(UUID tripId, UUID ownerId) {
        return UUID.nameUUIDFromBytes(
                (tripId + ":owner:" + ownerId).getBytes(StandardCharsets.UTF_8));
    }
}
