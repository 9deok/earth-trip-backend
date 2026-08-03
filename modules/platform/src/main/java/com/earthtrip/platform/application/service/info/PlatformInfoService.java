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

    PlatformInfoService(
        @Value("${earthtrip.capabilities.minimum-android-build:1}") int minimumAndroidBuild,
        @Value("${earthtrip.capabilities.minimum-ios-build:1}") int minimumIosBuild,
        @Value("${earthtrip.capabilities.maintenance-mode:false}") boolean maintenanceMode,
        @Value("${earthtrip.capabilities.maintenance-message:}") String maintenanceMessage,
        @Value("${earthtrip.capabilities.read-only-available:true}") boolean readOnlyAvailable,
        @Value("${earthtrip.providers.email.available:false}") boolean emailAvailable,
        @Value("${earthtrip.providers.places.available:false}") boolean placesAvailable,
        @Value("${earthtrip.providers.routes.available:false}") boolean routesAvailable,
        @Value("${earthtrip.providers.weather.available:false}") boolean weatherAvailable,
        @Value("${earthtrip.providers.exchange-rates.available:false}") boolean exchangeRatesAvailable,
        @Value("${earthtrip.providers.object-storage.available:false}") boolean objectStorageAvailable
    ) {
        this.minimumAndroidBuild = minimumAndroidBuild;
        this.minimumIosBuild = minimumIosBuild;
        this.maintenanceMode = maintenanceMode;
        this.maintenanceMessage = maintenanceMessage;
        this.readOnlyAvailable = readOnlyAvailable;
        this.emailAvailable = emailAvailable;
        this.placesAvailable = placesAvailable;
        this.routesAvailable = routesAvailable;
        this.weatherAvailable = weatherAvailable;
        this.exchangeRatesAvailable = exchangeRatesAvailable;
        this.objectStorageAvailable = objectStorageAvailable;
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
                provider("OBJECT_STORAGE", objectStorageAvailable)
            )
        );
    }

    @Override
    public List<CurrencyReference> currencies() {
        return Currency.getAvailableCurrencies().stream()
            .sorted(java.util.Comparator.comparing(Currency::getCurrencyCode))
            .map(currency -> new CurrencyReference(
                currency.getCurrencyCode(),
                Math.max(0, currency.getDefaultFractionDigits()),
                currency.getNumericCode(),
                currency.getDisplayName(Locale.KOREAN)
            ))
            .toList();
    }

    private static ProviderCapability provider(String name, boolean available) {
        return new ProviderCapability(
            name,
            available,
            available ? "AVAILABLE" : "PROVIDER_NOT_CONFIGURED"
        );
    }
}
