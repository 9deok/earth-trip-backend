package com.earthtrip.wallet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WalletRecordTest {

    @Test
    void 선택값이_null인_payload는_해당_필드를_제외하고_생성한다() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "여행자 보험 확인");
        payload.put("dueAt", null);
        payload.put("relatedReservationId", null);

        WalletRecord record = WalletRecord.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "PREPARATION_TASK",
            null,
            payload,
            "OPEN",
            "TRIP",
            0,
            UUID.randomUUID(),
            Instant.parse("2026-08-05T14:32:00Z")
        );

        assertThat(record.payload())
            .containsEntry("title", "여행자 보험 확인")
            .doesNotContainKeys("dueAt", "relatedReservationId");
        assertThatThrownBy(() -> record.payload().put("title", "변경"))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
