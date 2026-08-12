package com.earthtrip.platform.application.service.info;

import com.earthtrip.platform.application.port.in.PlatformInfoUseCase;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class PlatformInfoService implements PlatformInfoUseCase {

    private final int minimumAndroidBuild;
    private final int minimumIosBuild;
    private final boolean maintenanceMode;
    private final String maintenanceMessage;
    private final boolean readOnlyAvailable;
    private final boolean emailAvailable;
    private final boolean placesAvailable;
    private final boolean routesAvailable;
    private final boolean weatherAvailable;
    private final boolean exchangeRatesAvailable;
    private final boolean objectStorageAvailable;
    private final boolean pushAvailable;
    private final boolean calendarAvailable;
    private final boolean travelInformationAvailable;
    private final boolean transportStatusAvailable;
    private final boolean dataExportAvailable;

    PlatformInfoService(
            @Value("${earthtrip.capabilities.minimum-android-build:1}") int minimumAndroidBuild,
            @Value("${earthtrip.capabilities.minimum-ios-build:1}") int minimumIosBuild,
            @Value("${earthtrip.capabilities.maintenance-mode:false}") boolean maintenanceMode,
            @Value("${earthtrip.capabilities.maintenance-message:}") String maintenanceMessage,
            @Value("${earthtrip.capabilities.read-only-available:true}") boolean readOnlyAvailable,
            @Value("${earthtrip.providers.ses.region:ap-northeast-2}") String sesRegion,
            @Value("${earthtrip.providers.ses.from-email:}") String sesFromEmail,
            @Value("${earthtrip.providers.google-maps.api-key:}") String googleMapsApiKey,
            @Value("${earthtrip.storage.local.root:}") String storageRoot,
            @Value("${earthtrip.storage.local.signing-key:}") String storageSigningKey,
            @Value("${earthtrip.storage.clamav.endpoint:}") String clamAvEndpoint,
            @Value("${earthtrip.providers.firebase.project-id:}") String firebaseProjectId,
            @Value("${earthtrip.push.token-encryption-key:}") String pushTokenEncryptionKey,
            @Value("${earthtrip.providers.google-calendar.client-id:}") String calendarClientId,
            @Value("${earthtrip.providers.google-calendar.client-secret:}")
                    String calendarClientSecret,
            @Value("${earthtrip.providers.google-calendar.callback-uri:}")
                    String calendarCallbackUri,
            @Value("${earthtrip.integrations.encryption-keys:}") String integrationEncryptionKeys,
            @Value("${earthtrip.providers.mofa.service-key:}") String mofaServiceKey,
            @Value("${earthtrip.providers.amadeus.api-key:}") String amadeusApiKey,
            @Value("${earthtrip.providers.amadeus.api-secret:}") String amadeusApiSecret,
            @Value("${earthtrip.exports.local.root:}") String dataExportRoot) {
        this.minimumAndroidBuild = minimumAndroidBuild;
        this.minimumIosBuild = minimumIosBuild;
        this.maintenanceMode = maintenanceMode;
        this.maintenanceMessage = maintenanceMessage;
        this.readOnlyAvailable = readOnlyAvailable;
        this.emailAvailable = configured(sesRegion, sesFromEmail);
        boolean googleMapsAvailable = configured(googleMapsApiKey);
        this.placesAvailable = googleMapsAvailable;
        this.routesAvailable = googleMapsAvailable;
        this.weatherAvailable = googleMapsAvailable;
        this.exchangeRatesAvailable = true;
        this.objectStorageAvailable =
                configured(storageRoot, clamAvEndpoint) && validBase64Key(storageSigningKey);
        this.pushAvailable =
                configured(firebaseProjectId) && validBase64Key(pushTokenEncryptionKey);
        this.calendarAvailable =
                configured(calendarClientId, calendarClientSecret, calendarCallbackUri)
                        && hasValidKeyRingEntry(integrationEncryptionKeys);
        this.travelInformationAvailable = configured(mofaServiceKey);
        this.transportStatusAvailable = configured(amadeusApiKey, amadeusApiSecret);
        this.dataExportAvailable = configured(dataExportRoot);
    }

    @Override
    public AppCapabilities appCapabilities() {
        return new AppCapabilities(
                "v1",
                minimumAndroidBuild,
                minimumIosBuild,
                maintenanceMode,
                maintenanceMessage,
                readOnlyAvailable,
                List.of(
                        provider("EMAIL", emailAvailable),
                        provider("PLACES", placesAvailable),
                        provider("ROUTES", routesAvailable),
                        provider("WEATHER", weatherAvailable),
                        provider("EXCHANGE_RATES", exchangeRatesAvailable),
                        provider("OBJECT_STORAGE", objectStorageAvailable),
                        provider("LINK_PREVIEW", true),
                        provider("PUSH", pushAvailable),
                        provider("CALENDAR", calendarAvailable),
                        provider("TRAVEL_INFORMATION", travelInformationAvailable),
                        provider("TRANSPORT_STATUS", transportStatusAvailable),
                        provider("DATA_EXPORT", dataExportAvailable)));
    }

    @Override
    public List<CurrencyReference> currencies() {
        return Currency.getAvailableCurrencies().stream()
                .sorted(java.util.Comparator.comparing(Currency::getCurrencyCode))
                .map(
                        currency ->
                                new CurrencyReference(
                                        currency.getCurrencyCode(),
                                        Math.max(0, currency.getDefaultFractionDigits()),
                                        currency.getNumericCode(),
                                        currency.getDisplayName(Locale.KOREAN)))
                .toList();
    }

    @Override
    public List<CountryReference> countries() {
        return java.util.Arrays.stream(Locale.getISOCountries())
                .map(code -> Locale.of("", code))
                .sorted(
                        java.util.Comparator.comparing(
                                locale -> locale.getDisplayCountry(Locale.KOREAN)))
                .map(
                        locale ->
                                new CountryReference(
                                        locale.getCountry(),
                                        locale.getDisplayCountry(Locale.KOREAN),
                                        currencyCode(locale)))
                .toList();
    }

    private static String currencyCode(Locale locale) {
        try {
            Currency currency = Currency.getInstance(locale);
            return currency == null ? null : currency.getCurrencyCode();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static ProviderCapability provider(String name, boolean available) {
        return new ProviderCapability(
                name, available, available ? "AVAILABLE" : "PROVIDER_NOT_CONFIGURED");
    }

    private static boolean configured(String... values) {
        for (String value : values) {
            if (value == null || value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static boolean validBase64Key(String value) {
        try {
            return value != null
                    && !value.isBlank()
                    && java.util.Base64.getDecoder().decode(value.strip()).length == 32;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean hasValidKeyRingEntry(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (String entry : value.split(",")) {
            String[] pair = entry.strip().split(":", 2);
            if (pair.length == 2 && !pair[0].isBlank() && validBase64Key(pair[1])) {
                return true;
            }
        }
        return false;
    }
}
