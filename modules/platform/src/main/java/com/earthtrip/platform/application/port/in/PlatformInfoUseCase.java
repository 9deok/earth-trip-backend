package com.earthtrip.platform.application.port.in;

import java.util.List;

public interface PlatformInfoUseCase {

    AppCapabilities appCapabilities();

    List<CurrencyReference> currencies();

    List<CountryReference> countries();

    record AppCapabilities(
            String apiVersion,
            int minimumAndroidBuild,
            int minimumIosBuild,
            boolean maintenanceMode,
            String maintenanceMessage,
            boolean readOnlyAvailable,
            List<ProviderCapability> providers) {}

    record ProviderCapability(String provider, boolean available, String status) {}

    record CurrencyReference(
            String code, int fractionDigits, int numericCode, String displayName) {}

    record CountryReference(String code, String displayName, String currencyCode) {}
}
