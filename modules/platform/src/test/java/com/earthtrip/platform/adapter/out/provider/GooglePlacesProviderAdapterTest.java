package com.earthtrip.platform.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GooglePlacesProviderAdapterTest {

    @Test
    @SuppressWarnings("removal")
    void Google_검색_JSON을_도시명과_국가와_좌표로_변환한다() {
        RestClient.Builder builder =
                RestClient.builder()
                        .configureMessageConverters(
                                converters ->
                                        converters.withJsonConverter(
                                                new MappingJackson2HttpMessageConverter(
                                                        new ObjectMapper()
                                                                .findAndRegisterModules())));
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://places.googleapis.com/v1/places:searchText"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-Api-Key", "test-google-key"))
                .andExpect(
                        content()
                                .json(
                                        """
                {"textQuery":"로마","languageCode":"ko","pageSize":12}
                """))
                .andRespond(
                        withSuccess(
                                """
                {
                  "places": [
                    {
                      "id": "rome-place-id",
                      "displayName": {"text": "로마"},
                      "formattedAddress": "이탈리아 로마",
                      "addressComponents": [
                        {"longText": "이탈리아", "shortText": "IT", "types": ["country"]}
                      ],
                      "location": {"latitude": 41.9028, "longitude": 12.4964},
                      "types": ["locality", "political"]
                    }
                  ]
                }
                """,
                                MediaType.APPLICATION_JSON));
        GoogleMapsApiClient client = new GoogleMapsApiClient(builder.build(), "test-google-key");
        GooglePlacesProviderAdapter adapter =
                new GooglePlacesProviderAdapter(
                        client, Clock.fixed(Instant.parse("2026-08-05T14:19:25Z"), ZoneOffset.UTC));

        List<ProviderProxyUseCase.PlaceSummary> results =
                adapter.search("로마", "ko", null, null, 12);

        assertThat(results)
                .singleElement()
                .satisfies(
                        result -> {
                            assertThat(result.providerPlaceId()).isEqualTo("rome-place-id");
                            assertThat(result.name()).isEqualTo("로마");
                            assertThat(result.countryCode()).isEqualTo("IT");
                            assertThat(result.latitude()).isEqualByComparingTo("41.9028");
                            assertThat(result.longitude()).isEqualByComparingTo("12.4964");
                            assertThat(result.categories())
                                    .containsExactly("locality", "political");
                        });
        server.verify();
    }
}
