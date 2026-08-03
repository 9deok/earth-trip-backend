package com.earthtrip.platform.application.service.provider;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import com.earthtrip.platform.application.port.out.ExchangeRateProviderPort;
import com.earthtrip.platform.application.port.out.LinkPreviewProviderPort;
import com.earthtrip.platform.application.port.out.PlacesProviderPort;
import com.earthtrip.platform.application.port.out.RoutesProviderPort;
import com.earthtrip.platform.application.port.out.WeatherProviderPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
class ProviderProxyService implements ProviderProxyUseCase {

    private static final Set<String> ROUTE_MODES = Set.of(
        "WALKING", "DRIVING", "TRANSIT", "BICYCLING"
    );

    private final PlacesProviderPort places;
    private final RoutesProviderPort routes;
    private final WeatherProviderPort weather;
    private final ExchangeRateProviderPort exchangeRates;
    private final LinkPreviewProviderPort linkPreviews;

    ProviderProxyService(
        PlacesProviderPort places,
        RoutesProviderPort routes,
        WeatherProviderPort weather,
        ExchangeRateProviderPort exchangeRates,
        LinkPreviewProviderPort linkPreviews
    ) {
        this.places = places;
        this.routes = routes;
        this.weather = weather;
        this.exchangeRates = exchangeRates;
        this.linkPreviews = linkPreviews;
    }

    @Override
    public List<PlaceSummary> searchPlaces(
        String query,
        String language,
        BigDecimal nearLatitude,
        BigDecimal nearLongitude,
        Integer limit
    ) {
        String safeQuery = text(query, "PLACE_QUERY_REQUIRED", "검색어가 필요합니다.", 200);
        String safeLanguage = language(language);
        validateOptionalCoordinatePair(nearLatitude, nearLongitude);
        int safeLimit = limit == null ? 10 : limit;
        if (safeLimit < 1 || safeLimit > 20) {
            throw EarthTripException.badRequest(
                "INVALID_PLACE_SEARCH_LIMIT",
                "장소 검색 개수는 1에서 20 사이여야 합니다."
            );
        }
        return places.search(
            safeQuery, safeLanguage, nearLatitude, nearLongitude, safeLimit
        );
    }

    @Override
    public PlaceDetail place(String providerPlaceId, String language) {
        return places.detail(
            text(providerPlaceId, "PLACE_ID_REQUIRED", "장소 ID가 필요합니다.", 500),
            language(language)
        );
    }

    @Override
    public RouteResult route(RouteQuery query) {
        if (query == null) {
            throw EarthTripException.badRequest("ROUTE_QUERY_REQUIRED", "경로 요청이 필요합니다.");
        }
        validatePoint(query.origin());
        validatePoint(query.destination());
        List<RoutePoint> waypoints = query.waypoints() == null ? List.of() : query.waypoints();
        if (waypoints.size() > 10) {
            throw EarthTripException.badRequest(
                "TOO_MANY_ROUTE_WAYPOINTS",
                "경유지는 10개 이하여야 합니다."
            );
        }
        waypoints.forEach(ProviderProxyService::validatePoint);
        return routes.route(new RouteQuery(
            query.origin(), query.destination(), List.copyOf(waypoints),
            routeMode(query.mode()), query.departureAt()
        ));
    }

    @Override
    public MatrixResult routeMatrix(MatrixQuery query) {
        if (query == null || query.origins() == null || query.destinations() == null
            || query.origins().isEmpty() || query.destinations().isEmpty()) {
            throw EarthTripException.badRequest(
                "ROUTE_MATRIX_POINTS_REQUIRED",
                "출발지와 도착지가 각각 하나 이상 필요합니다."
            );
        }
        if ((long) query.origins().size() * query.destinations().size() > 100) {
            throw EarthTripException.badRequest(
                "ROUTE_MATRIX_TOO_LARGE",
                "경로 행렬은 출발지×도착지 100개 이하여야 합니다."
            );
        }
        query.origins().forEach(ProviderProxyService::validatePoint);
        query.destinations().forEach(ProviderProxyService::validatePoint);
        return routes.matrix(new MatrixQuery(
            List.copyOf(query.origins()), List.copyOf(query.destinations()),
            routeMode(query.mode()), query.departureAt()
        ));
    }

    @Override
    public List<WeatherDay> weather(WeatherQuery query) {
        if (query == null || query.startDate() == null || query.endDate() == null) {
            throw EarthTripException.badRequest(
                "WEATHER_QUERY_REQUIRED",
                "위치와 예보 날짜가 필요합니다."
            );
        }
        validateCoordinates(query.latitude(), query.longitude());
        long days = ChronoUnit.DAYS.between(query.startDate(), query.endDate());
        if (days < 0 || days > 14) {
            throw EarthTripException.badRequest(
                "INVALID_WEATHER_RANGE",
                "날씨 예보 범위는 시작일부터 14일 이하여야 합니다."
            );
        }
        String timeZone = text(
            query.timeZone(), "TIME_ZONE_REQUIRED", "IANA 시간대가 필요합니다.", 80
        );
        try {
            java.time.ZoneId.of(timeZone);
        } catch (java.time.DateTimeException exception) {
            throw EarthTripException.badRequest(
                "INVALID_TIME_ZONE",
                "유효한 IANA 시간대가 아닙니다."
            );
        }
        return weather.forecast(new WeatherQuery(
            query.latitude(), query.longitude(), query.startDate(), query.endDate(), timeZone
        ));
    }

    @Override
    public ExchangeRateResult exchangeRates(
        String baseCurrency,
        List<String> quoteCurrencies,
        Instant observedAt
    ) {
        String base = currency(baseCurrency);
        if (quoteCurrencies == null || quoteCurrencies.isEmpty()
            || quoteCurrencies.size() > 20) {
            throw EarthTripException.badRequest(
                "INVALID_QUOTE_CURRENCIES",
                "상대 통화는 1개 이상 20개 이하로 입력해 주세요."
            );
        }
        List<String> quotes = quoteCurrencies.stream().map(ProviderProxyService::currency).distinct().toList();
        if (quotes.contains(base)) {
            throw EarthTripException.badRequest(
                "SAME_EXCHANGE_CURRENCY",
                "기준 통화는 상대 통화 목록에 포함할 수 없습니다."
            );
        }
        return exchangeRates.rates(base, quotes, observedAt);
    }

    @Override
    public LinkPreviewResult linkPreview(String url) {
        return linkPreviews.preview(safeExternalUrl(url));
    }

    private static void validatePoint(RoutePoint point) {
        if (point == null) {
            throw EarthTripException.badRequest("ROUTE_POINT_REQUIRED", "경로 지점이 필요합니다.");
        }
        boolean hasPlaceId = point.providerPlaceId() != null
            && !point.providerPlaceId().isBlank();
        boolean hasCoordinates = point.latitude() != null || point.longitude() != null;
        if (hasPlaceId == hasCoordinates) {
            throw EarthTripException.badRequest(
                "INVALID_ROUTE_POINT",
                "경로 지점은 장소 ID 또는 좌표 중 하나만 사용해야 합니다."
            );
        }
        if (hasCoordinates) {
            validateCoordinates(point.latitude(), point.longitude());
        }
    }

    private static void validateOptionalCoordinatePair(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null && longitude == null) {
            return;
        }
        validateCoordinates(latitude, longitude);
    }

    private static void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null
            || latitude.compareTo(BigDecimal.valueOf(-90)) < 0
            || latitude.compareTo(BigDecimal.valueOf(90)) > 0
            || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
            || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw EarthTripException.badRequest(
                "INVALID_COORDINATES",
                "위도는 -90~90, 경도는 -180~180 사이여야 합니다."
            );
        }
    }

    private static String routeMode(String value) {
        String normalized = value == null ? "WALKING" : value.strip().toUpperCase(Locale.ROOT);
        if (!ROUTE_MODES.contains(normalized)) {
            throw EarthTripException.badRequest(
                "INVALID_ROUTE_MODE",
                "지원하지 않는 이동수단입니다."
            );
        }
        return normalized;
    }

    private static String language(String value) {
        String normalized = value == null || value.isBlank() ? "ko" : value.strip();
        if (!normalized.matches("[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})?")) {
            throw EarthTripException.badRequest(
                "INVALID_LANGUAGE",
                "언어 태그 형식을 확인해 주세요."
            );
        }
        return normalized;
    }

    private static String currency(String value) {
        try {
            return Currency.getInstance(value.strip().toUpperCase(Locale.ROOT)).getCurrencyCode();
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw EarthTripException.badRequest(
                "INVALID_CURRENCY",
                "유효한 ISO 4217 통화 코드가 아닙니다."
            );
        }
    }

    private static String safeExternalUrl(String value) {
        try {
            URI uri = new URI(text(
                value, "URL_REQUIRED", "미리 볼 URL이 필요합니다.", 2_048
            ));
            String scheme = uri.getScheme() == null
                ? ""
                : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if ((!scheme.equals("http") && !scheme.equals("https"))
                || host.isBlank() || uri.getUserInfo() != null
                || host.equals("localhost") || host.endsWith(".local")
                || host.matches("127\\..*") || host.matches("10\\..*")
                || host.matches("192\\.168\\..*") || host.matches("169\\.254\\..*")) {
                throw EarthTripException.badRequest(
                    "UNSAFE_PREVIEW_URL",
                    "외부 HTTP(S) URL만 미리 볼 수 있습니다."
                );
            }
            return uri.normalize().toString();
        } catch (URISyntaxException exception) {
            throw EarthTripException.badRequest(
                "INVALID_PREVIEW_URL",
                "URL 형식을 확인해 주세요."
            );
        }
    }

    private static String text(
        String value,
        String code,
        String message,
        int maximumLength
    ) {
        if (value == null || value.isBlank() || value.strip().length() > maximumLength) {
            throw EarthTripException.badRequest(code, message);
        }
        return value.strip();
    }
}
