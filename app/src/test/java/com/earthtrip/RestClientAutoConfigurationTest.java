package com.earthtrip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
        .withUserConfiguration(RestClientJsonConfiguration.class)
        .withBean(
            ObjectMapper.class,
            () -> new ObjectMapper().findAndRegisterModules()
        );

    @Test
    void 실행환경에_RestClient_Builder_빈을_제공한다() {
        contextRunner.run(context ->
            assertThat(context).hasSingleBean(RestClient.Builder.class)
        );
    }

    @Test
    void 운영_RestClient가_Jackson2_JsonNode로_Google_JSON을_변환한다() {
        contextRunner.run(context -> {
            RestClient.Builder builder = context.getBean(RestClient.Builder.class);
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo("https://places.googleapis.com/v1/places:searchText"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"textQuery\":\"로마\"}"))
                .andRespond(withSuccess(
                    """
                    {
                      "places": [
                        {
                          "id": "rome-place-id",
                          "displayName": {"text": "로마"},
                          "location": {"latitude": 41.9028, "longitude": 12.4964}
                        }
                      ]
                    }
                    """,
                    MediaType.APPLICATION_JSON
                ));

            JsonNode response = builder.build()
                .post()
                .uri("https://places.googleapis.com/v1/places:searchText")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("textQuery", "로마"))
                .retrieve()
                .body(JsonNode.class);

            assertThat(response).isNotNull();
            assertThat(response.path("places").path(0).path("id").asText())
                .isEqualTo("rome-place-id");
            assertThat(response.path("places").path(0).path("location").path("latitude")
                .asDouble()).isEqualTo(41.9028);
            server.verify();
        });
    }
}
