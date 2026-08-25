package com.earthtrip.platform.application.service.share;

import com.earthtrip.identity.api.TripMemberView;
import com.earthtrip.platform.application.port.out.TripShareStorePort;
import org.springframework.stereotype.Component;

@Component
class TripShareAuthorResolver {
    private final TripMemberView members;

    TripShareAuthorResolver(TripMemberView members) {
        this.members = members;
    }

    String displayName(TripShareStorePort.ShareRecord share) {
        try {
            return members.members(share.tripId(), share.createdBy()).stream()
                    .filter(member -> member.userId().equals(share.createdBy()))
                    .map(TripMemberView.Member::displayName)
                    .filter(name -> name != null && !name.isBlank())
                    .findFirst()
                    .orElse("여행자");
        } catch (RuntimeException ignored) {
            return "여행자";
        }
    }
}
