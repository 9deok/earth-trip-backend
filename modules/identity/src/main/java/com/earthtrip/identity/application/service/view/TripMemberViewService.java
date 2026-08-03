package com.earthtrip.identity.application.service.view;

import com.earthtrip.identity.api.TripMemberView;
import com.earthtrip.identity.application.port.in.TripMemberUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class TripMemberViewService implements TripMemberView {

    private final TripMemberUseCase members;

    TripMemberViewService(TripMemberUseCase members) {
        this.members = members;
    }

    @Override
    public List<Member> members(UUID tripId, UUID actorUserId) {
        return members.list(tripId, actorUserId).stream()
            .map(member -> new Member(
                member.memberId(), member.userId(), member.displayName(), member.email(),
                member.role(), member.status(), member.currentUser(), member.joinedAt(),
                member.version()
            ))
            .toList();
    }
}
