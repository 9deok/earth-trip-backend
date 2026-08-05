package com.earthtrip.platform.adapter.out.provider;

import com.earthtrip.platform.application.port.in.ExternalTravelUseCase;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
class MofaTravelInformationClient {

    private final RestClient restClient;
    private final Clock clock;
    private final String serviceKey;

    @Autowired
    MofaTravelInformationClient(
        RestClient.Builder builder,
        Clock clock,
        @Value("${earthtrip.providers.mofa.service-key:}") String serviceKey
    ) {
        this(builder.build(), clock, serviceKey);
    }

    MofaTravelInformationClient(RestClient restClient, Clock clock, String serviceKey) {
        this.restClient = restClient;
        this.clock = clock;
        this.serviceKey = serviceKey == null ? "" : serviceKey.strip();
    }

    boolean configured() {
        return !serviceKey.isBlank();
    }

    List<ExternalTravelUseCase.InformationResult> emergency(
        List<String> countryCodes,
        String language
    ) {
        requireConfigured();
        List<ExternalTravelUseCase.InformationResult> result = new ArrayList<>();
        for (String countryCode : countryCodes) {
            URI uri = commonUri(
                "https://apis.data.go.kr/1262000/CountrySafetyService6/getCountrySafetyList6",
                countryCode
            );
            for (JsonNode item : items(get(uri))) {
                result.add(new ExternalTravelUseCase.InformationResult(
                    first(item, "country_iso_alp2", "countryIsoAlp2", "countryCode", countryCode),
                    "SAFETY_NOTICE",
                    first(item, "title", "sj", "subject", "안전공지"),
                    clean(first(item, "txt_origin_cn", "content", "body", "")),
                    first(item, "file_url", "fileUrl", "url", "https://www.0404.go.kr/"),
                    "MOFA_COUNTRY_SAFETY",
                    observedAt(item)
                ));
            }
        }
        return List.copyOf(result);
    }

    List<ExternalTravelUseCase.InformationResult> advisories(
        List<String> countryCodes,
        String language
    ) {
        requireConfigured();
        List<ExternalTravelUseCase.InformationResult> result = new ArrayList<>();
        for (String countryCode : countryCodes) {
            URI uri = commonUri(
                "https://apis.data.go.kr/1262000/TravelAlarmService2/getTravelAlarmList2",
                countryCode
            );
            for (JsonNode item : items(get(uri))) {
                String level = first(item, "alarm_lvl", "alarmLevel", "level", "UNKNOWN");
                result.add(new ExternalTravelUseCase.InformationResult(
                    first(item, "country_iso_alp2", "countryIsoAlp2", "countryCode", countryCode),
                    "TRAVEL_ADVISORY_" + level,
                    "외교부 여행경보 " + level,
                    clean(first(item, "remark", "alarm_content", "content", "")),
                    first(
                        item,
                        "dang_map_download_url",
                        "downloadUrl",
                        "url",
                        "https://www.0404.go.kr/"
                    ),
                    "MOFA_TRAVEL_ALARM",
                    observedAt(item)
                ));
            }
        }
        return List.copyOf(result);
    }

    private URI commonUri(String baseUrl, String countryCode) {
        return UriComponentsBuilder.fromUriString(baseUrl)
            .queryParam("serviceKey", serviceKey)
            .queryParam("returnType", "JSON")
            .queryParam("pageNo", 1)
            .queryParam("numOfRows", 100)
            .queryParam("cond[country_iso_alp2::EQ]", countryCode)
            .build()
            .encode()
            .toUri();
    }

    private JsonNode get(URI uri) {
        try {
            JsonNode response = restClient.get().uri(uri).retrieve().body(JsonNode.class);
            if (response == null) {
                throw invalidResponse();
            }
            return response;
        } catch (RestClientException exception) {
            throw EarthTripException.unavailable(
                "MOFA_PROVIDER_UNAVAILABLE",
                "외교부 여행정보 제공자에 연결할 수 없습니다."
            );
        }
    }

    private static List<JsonNode> items(JsonNode response) {
        JsonNode values = response.path("data");
        if (!values.isArray()) {
            values = response.path("response").path("body").path("items").path("item");
        }
        if (values.isObject()) {
            return List.of(values);
        }
        if (!values.isArray()) {
            return List.of();
        }
        List<JsonNode> result = new ArrayList<>();
        values.forEach(result::add);
        return List.copyOf(result);
    }

    private Instant observedAt(JsonNode item) {
        String value = first(item, "wrt_dt", "wrtDt", "writtenAt", "");
        for (DateTimeFormatter formatter : List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
        )) {
            try {
                return LocalDate.parse(value, formatter).atStartOfDay().toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
                // 다음 제공자 날짜 형식을 확인한다.
            }
        }
        return clock.instant();
    }

    private static String first(
        JsonNode item,
        String first,
        String second,
        String third,
        String fallback
    ) {
        for (String field : List.of(first, second, third)) {
            String value = item.path(field).asText("");
            if (!value.isBlank()) {
                return value.strip();
            }
        }
        return fallback;
    }

    private static String clean(String html) {
        String value = html == null ? "" : Jsoup.parse(html).text();
        return value.length() <= 2_000 ? value : value.substring(0, 2_000);
    }

    private void requireConfigured() {
        if (!configured()) {
            throw EarthTripException.unavailable(
                "MOFA_PROVIDER_NOT_CONFIGURED",
                "외교부 공공데이터 서비스 키가 설정되지 않았습니다."
            );
        }
    }

    private static EarthTripException invalidResponse() {
        return new EarthTripException(
            "INVALID_MOFA_PROVIDER_RESPONSE",
            502,
            "외교부 여행정보 응답을 해석할 수 없습니다."
        );
    }
}
