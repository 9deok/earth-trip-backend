package com.earthtrip.platform.adapter.out.provider;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import com.earthtrip.platform.application.port.out.ExchangeRateProviderPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class EcbExchangeRateProviderAdapter implements ExchangeRateProviderPort {

    private static final String EUR = "EUR";
    private static final MathContext RATE_CONTEXT = MathContext.DECIMAL64;

    private final RestClient restClient;

    @Autowired
    EcbExchangeRateProviderAdapter(RestClient.Builder builder) {
        this(builder.build());
    }

    EcbExchangeRateProviderAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public ProviderProxyUseCase.ExchangeRateResult rates(
            String baseCurrency, List<String> quoteCurrencies, Instant observedAt) {
        Set<String> requested = new LinkedHashSet<>();
        if (!EUR.equals(baseCurrency)) {
            requested.add(baseCurrency);
        }
        quoteCurrencies.stream().filter(currency -> !EUR.equals(currency)).forEach(requested::add);
        Map<String, Observation> euroRates = fetch(requested, observedAt);
        BigDecimal euroToBase =
                EUR.equals(baseCurrency)
                        ? BigDecimal.ONE
                        : requireRate(euroRates, baseCurrency).value();
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (String quote : quoteCurrencies) {
            BigDecimal euroToQuote =
                    EUR.equals(quote) ? BigDecimal.ONE : requireRate(euroRates, quote).value();
            result.put(quote, euroToQuote.divide(euroToBase, RATE_CONTEXT));
        }
        LocalDate latestDate =
                euroRates.values().stream()
                        .map(Observation::date)
                        .max(Comparator.naturalOrder())
                        .orElseGet(() -> LocalDate.now(ZoneOffset.UTC));
        return new ProviderProxyUseCase.ExchangeRateResult(
                baseCurrency,
                Map.copyOf(result),
                "ECB_REFERENCE_RATE",
                latestDate.atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    private Map<String, Observation> fetch(Set<String> currencies, Instant requestedAt) {
        if (currencies.isEmpty()) {
            return Map.of();
        }
        String currencyPath = String.join("+", currencies);
        StringBuilder url =
                new StringBuilder("https://data-api.ecb.europa.eu/service/data/EXR/D.")
                        .append(currencyPath)
                        .append(".EUR.SP00.A?format=csvdata&detail=dataonly");
        if (requestedAt == null) {
            url.append("&lastNObservations=1");
        } else {
            LocalDate date = LocalDate.ofInstant(requestedAt, ZoneOffset.UTC);
            url.append("&startPeriod=").append(date.minusDays(7));
            url.append("&endPeriod=").append(date);
        }
        String csv;
        try {
            csv =
                    restClient
                            .get()
                            .uri(URI.create(url.toString()))
                            .accept(MediaType.parseMediaType("text/csv"))
                            .retrieve()
                            .body(String.class);
        } catch (RestClientException exception) {
            throw EarthTripException.unavailable(
                    "EXCHANGE_RATE_PROVIDER_UNAVAILABLE", "ECB 환율 제공자에 연결할 수 없습니다.");
        }
        return parse(csv);
    }

    private static Map<String, Observation> parse(String csv) {
        if (csv == null || csv.isBlank()) {
            throw invalidResponse();
        }
        String[] lines = csv.split("\\R");
        List<String> headers = csvRow(lines[0]);
        int currencyIndex = headers.indexOf("CURRENCY");
        int dateIndex = headers.indexOf("TIME_PERIOD");
        int valueIndex = headers.indexOf("OBS_VALUE");
        if (currencyIndex < 0 || dateIndex < 0 || valueIndex < 0) {
            throw invalidResponse();
        }
        Map<String, Observation> observations = new LinkedHashMap<>();
        for (int lineIndex = 1; lineIndex < lines.length; lineIndex++) {
            if (lines[lineIndex].isBlank()) {
                continue;
            }
            List<String> columns = csvRow(lines[lineIndex]);
            if (columns.size() <= Math.max(currencyIndex, Math.max(dateIndex, valueIndex))) {
                continue;
            }
            try {
                String currency = columns.get(currencyIndex);
                Observation candidate =
                        new Observation(
                                LocalDate.parse(columns.get(dateIndex)),
                                new BigDecimal(columns.get(valueIndex)));
                observations.merge(
                        currency,
                        candidate,
                        (oldValue, newValue) ->
                                newValue.date().isAfter(oldValue.date()) ? newValue : oldValue);
            } catch (java.time.DateTimeException | NumberFormatException ignored) {
                // 손상된 행은 제외하고 필요한 통화가 남지 않으면 아래에서 fail closed 한다.
            }
        }
        return Map.copyOf(observations);
    }

    private static List<String> csvRow(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(character);
            }
        }
        values.add(value.toString());
        return List.copyOf(values);
    }

    private static Observation requireRate(Map<String, Observation> rates, String currency) {
        Observation result = rates.get(currency);
        if (result == null) {
            throw new EarthTripException(
                    "EXCHANGE_RATE_NOT_AVAILABLE",
                    502,
                    "ECB에서 요청한 통화의 기준 환율을 제공하지 않습니다.",
                    Map.of("currency", currency));
        }
        return result;
    }

    private static EarthTripException invalidResponse() {
        return new EarthTripException(
                "INVALID_EXCHANGE_RATE_PROVIDER_RESPONSE", 502, "ECB 환율 응답을 해석할 수 없습니다.");
    }

    private record Observation(LocalDate date, BigDecimal value) {}
}
