package com.earthtrip.identity.application.service.preference;

import com.earthtrip.identity.application.port.in.PreferenceUseCase;
import com.earthtrip.identity.application.port.out.PreferenceStorePort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class PreferenceService implements PreferenceUseCase {

    private final PreferenceStorePort store;
    private final Clock clock;

    PreferenceService(PreferenceStorePort store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Override
    public PreferenceResult get(UUID userId) {
        return result(loadOrCreate(userId));
    }

    @Override
    public PreferenceResult update(UUID userId, UpdatePreferenceCommand command) {
        PreferenceStorePort.PreferenceRecord current = loadOrCreate(userId);
        String locale = command.locale() == null ? current.locale() : validLocale(command.locale());
        String currency = command.defaultCurrency() == null
            ? current.defaultCurrency()
            : validCurrency(command.defaultCurrency());
        String timeZone = command.timeZone() == null
            ? current.timeZone()
            : validTimeZone(command.timeZone());
        PreferenceStorePort.PreferenceRecord updated = new PreferenceStorePort.PreferenceRecord(
            userId,
            locale,
            currency,
            timeZone,
            valueOr(command.shareTicketNames(), current.shareTicketNames()),
            valueOr(command.sharePersonalExpense(), current.sharePersonalExpense()),
            valueOr(command.optionalAnalytics(), current.optionalAnalytics()),
            current.version(),
            current.createdAt(),
            clock.instant()
        );
        return result(store.save(updated));
    }

    private PreferenceStorePort.PreferenceRecord loadOrCreate(UUID userId) {
        return store.find(userId).orElseGet(() -> {
            Instant now = clock.instant();
            return store.save(new PreferenceStorePort.PreferenceRecord(
                userId, "ko-KR", "KRW", "Asia/Seoul", false, false, false, 0, now, now
            ));
        });
    }

    private static boolean valueOr(Boolean candidate, boolean fallback) {
        return candidate == null ? fallback : candidate;
    }

    private static String validLocale(String value) {
        String normalized = value.strip();
        Locale locale = Locale.forLanguageTag(normalized);
        if (normalized.isBlank() || locale.getLanguage().isBlank()) {
            throw new IllegalArgumentException("유효한 BCP 47 언어 태그가 필요합니다.");
        }
        return locale.toLanguageTag();
    }

    private static String validCurrency(String value) {
        return Currency.getInstance(value.strip().toUpperCase(Locale.ROOT)).getCurrencyCode();
    }

    private static String validTimeZone(String value) {
        return ZoneId.of(value.strip()).getId();
    }

    private static PreferenceResult result(PreferenceStorePort.PreferenceRecord preference) {
        return new PreferenceResult(
            preference.locale(),
            preference.defaultCurrency(),
            preference.timeZone(),
            preference.shareTicketNames(),
            preference.sharePersonalExpense(),
            preference.optionalAnalytics(),
            preference.version(),
            preference.updatedAt()
        );
    }
}
