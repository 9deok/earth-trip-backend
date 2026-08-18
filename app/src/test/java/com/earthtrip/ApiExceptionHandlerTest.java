package com.earthtrip;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

class ApiExceptionHandlerTest {

    @Test
    void 서버_오류는_traceId와_원인_예외_스택을_함께_기록한다() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        MockHttpServletRequest request = request();
        IllegalStateException cause = new IllegalStateException("provider timeout");
        EarthTripException exception =
                EarthTripException.unavailable("PLACES_UNAVAILABLE", "장소 서버에 연결할 수 없습니다.", cause);
        CapturedLogs logs = captureLogs();

        ResponseEntity<?> response;
        try {
            response = handler.handleEarthTrip(exception, request);
        } finally {
            logs.close();
        }

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(logs.events()).hasSize(1);
        ILoggingEvent event = logs.events().getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage())
                .contains(
                        "code=PLACES_UNAVAILABLE",
                        "status=503",
                        "route=/api/v1/places/search",
                        "traceId=trace-api-error");
        assertThat(event.getThrowableProxy()).isNotNull();
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void 예상하지_못한_예외는_안전한_500_응답과_스택을_남긴다() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        MockHttpServletRequest request = request();
        CapturedLogs logs = captureLogs();

        ResponseEntity<?> response;
        try {
            response =
                    handler.handleUnexpected(new NullPointerException("internal detail"), request);
        } finally {
            logs.close();
        }

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().toString()).doesNotContain("internal detail");
        assertThat(logs.events().getFirst().getThrowableProxy()).isNotNull();
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/places/search");
        request.setAttribute("earthTripTraceId", "trace-api-error");
        request.setAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/places/search");
        return request;
    }

    private static CapturedLogs captureLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(ApiExceptionHandler.class);
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
