package com.earthtrip.platform.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class EcbExchangeRateProviderAdapterTest {

    @Test
    void convertsEcbEuroReferenceRatesToRequestedCrossRate() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(
                        requestTo(
                                org.hamcrest.Matchers.containsString(
                                        "data-api.ecb.europa.eu/service/data/EXR/D.")))
                .andRespond(
                        withSuccess(
                                """
            CURRENCY,TIME_PERIOD,OBS_VALUE
            USD,2026-08-03,1.2000
            KRW,2026-08-03,1600.0000
            """,
                                MediaType.parseMediaType("text/csv")));
        EcbExchangeRateProviderAdapter adapter =
                new EcbExchangeRateProviderAdapter(builder.build());

        var result =
                adapter.rates("USD", List.of("KRW", "EUR"), Instant.parse("2026-08-03T00:00:00Z"));

        assertThat(result.rates().get("KRW"))
                .isEqualByComparingTo(new BigDecimal("1333.333333333333"));
        assertThat(result.rates().get("EUR"))
                .isEqualByComparingTo(new BigDecimal("0.8333333333333333"));
        assertThat(result.source()).isEqualTo("ECB_REFERENCE_RATE");
        server.verify();
    }
}
