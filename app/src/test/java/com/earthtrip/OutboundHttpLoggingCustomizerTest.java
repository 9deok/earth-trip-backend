package com.earthtrip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OutboundHttpLoggingCustomizerTest {

    @Test
    void 외부_HTTP는_query와_사진_식별자를_숨기고_상태와_지연시간을_기록한다() {
        String target =
                "https://places.googleapis.com/v1/places/place-1/photos/photo-secret/media"
                        + "?key=google-api-secret";
        RestClient.Builder builder = RestClient.builder();
        new OutboundHttpLoggingCustomizer().customize(builder);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(target)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        CapturedLogs logs = captureLogs();

        try {
            builder.build().get().uri(target).retrieve().body(String.class);
        } finally {
            logs.close();
        }

        server.verify();
        assertThat(logs.events()).hasSize(1);
        ILoggingEvent event = logs.events().getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage())
                .contains(
                        "OUTBOUND_HTTP GET",
                        "places.googleapis.com/v1/places/place-1/photos/[REDACTED]/media",
                        "-> 200",
                        "requestBytes=0")
                .doesNotContain("photo-secret", "google-api-secret", "?key=");
    }

    private static CapturedLogs captureLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(OutboundHttpLoggingCustomizer.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return new CapturedLogs(logger, appender);
    }

    private record CapturedLogs(Logger logger, ListAppender<ILoggingEvent> appender) {
        List<ILoggingEvent> events() {
            return appender.list;
        }

        void close() {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
