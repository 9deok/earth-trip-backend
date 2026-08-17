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
                      "types": ["locality", "political"],
                      "photos": [{
                        "name": "places/rome/photos/photo-1",
                        "authorAttributions": [{
                          "displayName": "Roma Travel",
                          "uri": "https://example.com/author"
                        }]
                      }],
                      "rating": 4.7,
                      "userRatingCount": 18234,
                      "priceLevel": "PRICE_LEVEL_MODERATE",
                      "currentOpeningHours": {"openNow": true},
                      "googleMapsUri": "https://maps.google.com/?cid=rome"
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
                            assertThat(result.photoName()).isEqualTo("places/rome/photos/photo-1");
                            assertThat(result.photoAttributions())
                                    .singleElement()
                                    .extracting(
                                            ProviderProxyUseCase.PlacePhotoAttribution::displayName)
                                    .isEqualTo("Roma Travel");
                            assertThat(result.rating()).isEqualByComparingTo("4.7");
                            assertThat(result.userRatingCount()).isEqualTo(18_234);
                            assertThat(result.priceLevel()).isEqualTo("PRICE_LEVEL_MODERATE");
                            assertThat(result.openNow()).isTrue();
                            assertThat(result.googleMapsUrl())
                                    .isEqualTo("https://maps.google.com/?cid=rome");
                        });
        server.verify();
    }

    @Test
    @SuppressWarnings("removal")
    void 사진_이름을_Google_Photo_Media로_해석해_이미지_바이트를_반환한다() {
        RestClient.Builder builder =
                RestClient.builder()
                        .configureMessageConverters(
                                converters ->
                                        converters.withJsonConverter(
                                                new MappingJackson2HttpMessageConverter(
                                                        new ObjectMapper()
                                                                .findAndRegisterModules())));
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(
                        requestTo(
                                "https://places.googleapis.com/v1/places/rome/photos/photo-1/media"
                                        + "?skipHttpRedirect=true&maxWidthPx=960"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Goog-Api-Key", "test-google-key"))
                .andRespond(
                        withSuccess(
                                """
                {"photoUri":"https://lh3.googleusercontent.com/place-photo"}
                """,
                                MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://lh3.googleusercontent.com/place-photo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new byte[] {1, 2, 3}, MediaType.IMAGE_JPEG));
        GooglePlacesProviderAdapter adapter =
                new GooglePlacesProviderAdapter(
                        new GoogleMapsApiClient(builder.build(), "test-google-key"),
                        Clock.fixed(Instant.parse("2026-08-05T14:19:25Z"), ZoneOffset.UTC));

        ProviderProxyUseCase.PlacePhoto photo =
                adapter.photo("places/rome/photos/photo-1", 960, null);

        assertThat(photo.contentType()).isEqualTo("image/jpeg");
        assertThat(photo.bytes()).containsExactly(1, 2, 3);
        server.verify();
    }
}
