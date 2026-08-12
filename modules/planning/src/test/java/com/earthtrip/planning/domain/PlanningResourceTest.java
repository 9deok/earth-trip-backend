package com.earthtrip.planning.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlanningResourceTest {

    @Test
    void 선택값이_null인_payload는_해당_필드를_제외하고_생성한다() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "로마 일정 시작");
        payload.put("placeId", null);
        payload.put("timeBand", null);

        PlanningResource resource =
                PlanningResource.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "SCHEDULE_ITEM",
                        UUID.randomUUID(),
                        LocalDate.of(2026, 9, 4),
                        payload,
                        "PLANNED",
                        0,
                        UUID.randomUUID(),
                        Instant.parse("2026-08-05T14:32:03Z"));

        assertThat(resource.payload())
                .containsEntry("title", "로마 일정 시작")
                .doesNotContainKeys("placeId", "timeBand");
        assertThatThrownBy(() -> resource.payload().put("title", "변경"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
