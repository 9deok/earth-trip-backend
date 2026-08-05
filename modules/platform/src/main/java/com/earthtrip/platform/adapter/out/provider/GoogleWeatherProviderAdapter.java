package com.earthtrip.platform.adapter.out.provider;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import com.earthtrip.platform.application.port.out.WeatherProviderPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
class GoogleWeatherProviderAdapter implements WeatherProviderPort {

    private final GoogleMapsApiClient client;
    private final Clock clock;

    GoogleWeatherProviderAdapter(GoogleMapsApiClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    @Override
    public List<ProviderProxyUseCase.WeatherDay> forecast(
        ProviderProxyUseCase.WeatherQuery query
    ) {
        LocalDate today = LocalDate.now(clock.withZone(java.time.ZoneId.of(query.timeZone())));
        if (query.startDate().isBefore(today)
            || query.endDate().isAfter(today.plusDays(9))) {
            throw EarthTripException.badRequest(
                "WEATHER_DATE_OUT_OF_FORECAST_RANGE",
                "Google Weather 예보는 현지 오늘부터 최대 10일까지 조회할 수 있습니다."
            );
        }
        int days = Math.toIntExact(ChronoUnit.DAYS.between(today, query.endDate())) + 1;
        URI uri = UriComponentsBuilder
            .fromUriString("https://weather.googleapis.com/v1/forecast/days:lookup")
            .queryParam("location.latitude", query.latitude())
            .queryParam("location.longitude", query.longitude())
            .queryParam("days", days)
            .queryParam("unitsSystem", "METRIC")
            .queryParam("languageCode", "ko")
            .build(true)
            .toUri();
        JsonNode response = client.get(uri, null, "WEATHER_PROVIDER");
        List<ProviderProxyUseCase.WeatherDay> result = new ArrayList<>();
        for (JsonNode day : response.path("forecastDays")) {
            LocalDate date = date(day.path("displayDate"));
            if (date == null || date.isBefore(query.startDate()) || date.isAfter(query.endDate())) {
                continue;
            }
            JsonNode daytime = day.path("daytimeForecast");
            JsonNode nighttime = day.path("nighttimeForecast");
            result.add(new ProviderProxyUseCase.WeatherDay(
                date,
                maximum(
                    percent(daytime.path("precipitation").path("probability")),
                    percent(nighttime.path("precipitation").path("probability"))
                ),
                degrees(day.path("minTemperature")),
                degrees(day.path("maxTemperature")),
                daytime.path("weatherCondition").path("type").asText("UNKNOWN"),
                "GOOGLE_WEATHER",
                clock.instant()
            ));
        }
        result.sort(Comparator.comparing(ProviderProxyUseCase.WeatherDay::localDate));
        return List.copyOf(result);
    }

    private static LocalDate date(JsonNode node) {
        int year = node.path("year").asInt();
        int month = node.path("month").asInt();
        int day = node.path("day").asInt();
        try {
            return LocalDate.of(year, month, day);
        } catch (java.time.DateTimeException exception) {
            return null;
        }
    }

    private static BigDecimal degrees(JsonNode node) {
        return node.has("degrees") && node.get("degrees").isNumber()
            ? node.get("degrees").decimalValue()
            : null;
    }

    private static BigDecimal percent(JsonNode node) {
        return node.has("percent") && node.get("percent").isNumber()
            ? node.get("percent").decimalValue().movePointLeft(2)
            : BigDecimal.ZERO;
    }

    private static BigDecimal maximum(BigDecimal first, BigDecimal second) {
        return first.max(second);
    }
}
