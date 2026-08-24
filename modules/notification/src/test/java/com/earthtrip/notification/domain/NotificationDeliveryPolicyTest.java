package com.earthtrip.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class NotificationDeliveryPolicyTest {

    @Test
    void 비활성화한_알림_범주는_푸시하지_않는다() {
        var preference = preference(true, null, null);

        assertThat(
                        NotificationDeliveryPolicy.allowsPush(
                                "SCHEDULE_CHANGED",
                                new NotificationDeliveryPolicy.Preference(
                                        true, false, true, true, true, null, null, "Asia/Seoul"),
                                Instant.parse("2026-08-24T01:00:00Z")))
                .isFalse();
        assertThat(
                        NotificationDeliveryPolicy.allowsPush(
                                "MENTION_CREATED",
                                preference,
                                Instant.parse("2026-08-24T01:00:00Z")))
                .isTrue();
    }

    @Test
    void 자정을_넘는_방해금지_시간을_처리한다() {
        var preference = preference(true, LocalTime.of(22, 0), LocalTime.of(7, 0));

        assertThat(
                        NotificationDeliveryPolicy.allowsPush(
                                "MENTION_CREATED",
                                preference,
                                Instant.parse("2026-08-24T14:30:00Z")))
                .isFalse();
        assertThat(
                        NotificationDeliveryPolicy.allowsPush(
                                "MENTION_CREATED",
                                preference,
                                Instant.parse("2026-08-24T03:00:00Z")))
                .isTrue();
    }

    private static NotificationDeliveryPolicy.Preference preference(
            boolean push, LocalTime quietStart, LocalTime quietEnd) {
        return new NotificationDeliveryPolicy.Preference(
                true, true, true, true, push, quietStart, quietEnd, "Asia/Seoul");
    }
}
