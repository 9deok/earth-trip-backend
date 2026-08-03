package com.earthtrip.identity.adapter.out.persistence.preference;

import com.earthtrip.identity.application.port.out.PreferenceStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "user_preferences")
class PreferenceJpaEntity {

    @Id
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "locale", nullable = false, length = 20)
    private String locale;

    @Column(name = "default_currency", nullable = false, length = 3)
    private String defaultCurrency;

    @Column(name = "time_zone", nullable = false, length = 80)
    private String timeZone;

    @Column(name = "share_ticket_names", nullable = false)
    private boolean shareTicketNames;

    @Column(name = "share_personal_expense", nullable = false)
    private boolean sharePersonalExpense;

    @Column(name = "optional_analytics", nullable = false)
    private boolean optionalAnalytics;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PreferenceJpaEntity() { }

    PreferenceJpaEntity(PreferenceStorePort.PreferenceRecord record) {
        apply(record);
        this.version = record.version();
    }

    void apply(PreferenceStorePort.PreferenceRecord record) {
        this.userId = record.userId().toString();
        this.locale = record.locale();
        this.defaultCurrency = record.defaultCurrency();
        this.timeZone = record.timeZone();
        this.shareTicketNames = record.shareTicketNames();
        this.sharePersonalExpense = record.sharePersonalExpense();
        this.optionalAnalytics = record.optionalAnalytics();
        this.createdAt = record.createdAt();
        this.updatedAt = record.updatedAt();
    }

    PreferenceStorePort.PreferenceRecord toRecord() {
        return new PreferenceStorePort.PreferenceRecord(
            java.util.UUID.fromString(userId), locale, defaultCurrency, timeZone,
            shareTicketNames, sharePersonalExpense, optionalAnalytics, version, createdAt, updatedAt
        );
    }
}
