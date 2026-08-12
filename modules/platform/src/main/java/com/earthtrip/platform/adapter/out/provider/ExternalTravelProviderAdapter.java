package com.earthtrip.platform.adapter.out.provider;

import com.earthtrip.platform.application.port.in.ExternalTravelUseCase;
import com.earthtrip.platform.application.port.out.ExternalTravelProviderPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class ExternalTravelProviderAdapter implements ExternalTravelProviderPort {

    private final GoogleGeoTravelClient google;
    private final AmadeusTravelClient amadeus;
    private final MofaTravelInformationClient mofa;

    ExternalTravelProviderAdapter(
            GoogleGeoTravelClient google,
            AmadeusTravelClient amadeus,
            MofaTravelInformationClient mofa) {
        this.google = google;
        this.amadeus = amadeus;
        this.mofa = mofa;
    }

    @Override
    public List<ExternalTravelUseCase.GeoResult> forward(String query, String language, int limit) {
        return google.forward(query, language, limit);
    }

    @Override
    public ExternalTravelUseCase.GeoResult reverse(
            BigDecimal latitude, BigDecimal longitude, String language) {
        return google.reverse(latitude, longitude, language);
    }

    @Override
    public ExternalTravelUseCase.PlaceUrlResult resolvePlaceUrl(String url, String language) {
        return google.resolve(url, language);
    }

    @Override
    public ExternalTravelUseCase.TimeZoneResult timeZone(
            BigDecimal latitude, BigDecimal longitude) {
        return google.timeZone(latitude, longitude);
    }

    @Override
    public List<ExternalTravelUseCase.TransportStatusResult> transportStatuses(
            List<String> references, Instant observedAt) {
        return amadeus.statuses(references, observedAt);
    }

    @Override
    public List<ExternalTravelUseCase.InformationResult> emergency(
            List<String> countryCodes, String language) {
        return mofa.emergency(countryCodes, language);
    }

    @Override
    public List<ExternalTravelUseCase.InformationResult> advisories(
            List<String> countryCodes, String language) {
        return mofa.advisories(countryCodes, language);
    }

    @Override
    public Map<String, Object> refreshComparison(Map<String, Object> current) {
        return amadeus.refreshComparison(current);
    }
}
