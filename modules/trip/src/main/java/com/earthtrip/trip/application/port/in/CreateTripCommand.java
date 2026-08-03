package com.earthtrip.trip.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record CreateTripCommand(
    UUID requestId,
    UUID ownerUserId,
    String title,
    String timeZone,
    String defaultCurrency
) {

    public CreateTripCommand {
        Objects.requireNonNull(requestId, "요청 ID는 필수입니다.");
        Objects.requireNonNull(ownerUserId, "소유자 ID는 필수입니다.");
        Objects.requireNonNull(title, "여행 이름은 필수입니다.");
    }
}
