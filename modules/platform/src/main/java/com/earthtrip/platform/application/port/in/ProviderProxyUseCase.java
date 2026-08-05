package com.earthtrip.platform.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ProviderProxyUseCase {

    List<PlaceSummary> searchPlaces(
        String query,
        String language,
        BigDecimal nearLatitude,
        BigDecimal nearLongitude,
        Integer limit
    );

    PlaceDetail place(String providerPlaceId, String language);

    RouteResult route(RouteQuery query);

    MatrixResult routeMatrix(MatrixQuery query);

    List<WeatherDay> weather(WeatherQuery query);

    ExchangeRateResult exchangeRates(
        String baseCurrency,
        List<String> quoteCurrencies,
        Instant observedAt
    );

    LinkPreviewResult linkPreview(String url);

    record PlaceSummary(
        String providerPlaceId,
        String name,
        String formattedAddress,
        String countryCode,
        BigDecimal latitude,
        BigDecimal longitude,
        List<String> categories,
        String source
    ) { }

    record PlaceDetail(
        String providerPlaceId,
        String name,
        String formattedAddress,
        String countryCode,
        BigDecimal latitude,
        BigDecimal longitude,
        List<String> categories,
        Map<String, List<OpeningInterval>> openingHours,
        String websiteUrl,
        String phoneNumber,
        String source,
        Instant fetchedAt
    ) { }

    record OpeningInterval(int openMinute, int closeMinute) { }

    record RouteQuery(
        RoutePoint origin,
        RoutePoint destination,
        List<RoutePoint> waypoints,
        String mode,
        Instant departureAt
    ) { }

    record MatrixQuery(
        List<RoutePoint> origins,
        List<RoutePoint> destinations,
        String mode,
        Instant departureAt
    ) { }

    record RoutePoint(BigDecimal latitude, BigDecimal longitude, String providerPlaceId) { }

    record RouteResult(
        long distanceMeters,
        int durationSeconds,
        String encodedPolyline,
        String mode,
        String source,
        Instant calculatedAt
    ) { }

    record MatrixResult(
        List<MatrixCell> cells,
        String mode,
        String source,
        Instant calculatedAt
    ) { }

    record MatrixCell(
        int originIndex,
        int destinationIndex,
        Long distanceMeters,
        Integer durationSeconds,
        String status
    ) { }

    record WeatherQuery(
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDate startDate,
        LocalDate endDate,
        String timeZone
    ) { }

    record WeatherDay(
        LocalDate localDate,
        BigDecimal precipitationProbability,
        BigDecimal minimumTemperatureCelsius,
        BigDecimal maximumTemperatureCelsius,
        String condition,
        String source,
        Instant observedAt
    ) { }

    record ExchangeRateResult(
        String baseCurrency,
        Map<String, BigDecimal> rates,
        String source,
        Instant observedAt
    ) { }

    record LinkPreviewResult(
        String canonicalUrl,
        String title,
        String description,
        String imageUrl,
        String siteName,
        String source,
        Instant fetchedAt
    ) { }
}
