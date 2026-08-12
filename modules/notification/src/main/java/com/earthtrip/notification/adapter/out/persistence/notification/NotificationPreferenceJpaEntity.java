package com.earthtrip.notification.adapter.out.persistence.notification;

import com.earthtrip.notification.application.port.out.NotificationStoreRecords;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
class NotificationPreferenceJpaEntity {
    @Id
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "mentions_enabled", nullable = false)
    private boolean mentions;

    @Column(name = "schedule_enabled", nullable = false)
    private boolean schedule;

    @Column(name = "expense_enabled", nullable = false)
    private boolean expense;

    @Column(name = "invitation_enabled", nullable = false)
    private boolean invitation;

    @Column(name = "push_enabled", nullable = false)
    private boolean push;

    @Column(name = "email_enabled", nullable = false)
    private boolean email;

    @Column(name = "quiet_start")
    private LocalTime quietStart;

    @Column(name = "quiet_end")
    private LocalTime quietEnd;

    @Column(name = "quiet_time_zone", length = 80)
    private String quietTimeZone;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected NotificationPreferenceJpaEntity() {}

    void apply(NotificationStoreRecords.PreferenceRecord r) {
        userId = r.userId().toString();
        mentions = r.mentions();
        schedule = r.schedule();
        expense = r.expense();
        invitation = r.invitation();
        push = r.push();
        email = r.email();
        quietStart = r.quietStart();
        quietEnd = r.quietEnd();
        quietTimeZone = r.quietTimeZone();
        updatedAt = r.updatedAt();
    }

    NotificationStoreRecords.PreferenceRecord record() {
        return new NotificationStoreRecords.PreferenceRecord(
                UUID.fromString(userId),
                mentions,
                schedule,
                expense,
                invitation,
                push,
                email,
                quietStart,
                quietEnd,
                quietTimeZone,
                version,
                updatedAt);
    }
}
