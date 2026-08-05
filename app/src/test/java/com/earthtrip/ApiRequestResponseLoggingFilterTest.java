package com.earthtrip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

class ApiRequestResponseLoggingFilterTest {

    @Test
    void JSON_request와_response를_기록하되_인증정보와_비밀번호는_가린다() throws Exception {
        ApiRequestResponseLoggingFilter filter = new ApiRequestResponseLoggingFilter(
            new ObjectMapper(),
            4096
        );
        MockHttpServletRequest request = new MockHttpServletRequest(
            "POST",
            "/api/v1/invitations/path-secret/acceptances"
        );
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setQueryString("page=1&token=query-secret");
        request.addHeader("Authorization", "Bearer header-secret");
        request.addHeader("User-Agent", "earth-trip-test");
        request.addHeader("Cf-Ray", "cloudflare-noise");
        request.setAttribute("earthTripTraceId", "trace-123");
        request.setAttribute(
            HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
            "/api/v1/invitations/{token}/acceptances"
        );
        request.setContent(("{\"email\":\"traveler@example.com\","
            + "\"password\":\"request-password\","
            + "\"nested\":{\"accessToken\":\"request-token\"}}").getBytes(
                StandardCharsets.UTF_8
            ));
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturedLogs logs = captureLogs();

        try {
            filter.doFilter(request, response, (wrappedRequest, wrappedResponse) -> {
                wrappedRequest.getInputStream().readAllBytes();
                HttpServletResponse httpResponse = (HttpServletResponse) wrappedResponse;
                httpResponse.setStatus(201);
                httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
                httpResponse.setHeader("Set-Cookie", "session=response-cookie-secret");
                httpResponse.getWriter().write(
                    "{\"displayName\":\"Earth Traveler\","
                        + "\"sessionToken\":\"response-token\"}"
                );
            });
        } finally {
            logs.close();
        }

        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getContentAsString()).contains("response-token");
        assertThat(logs.events()).hasSize(1);
        String message = logs.events().getFirst().getFormattedMessage();
        assertThat(logs.events().getFirst().getLevel()).isEqualTo(Level.INFO);
        assertThat(message)
            .startsWith(
                "HTTP_EXCHANGE POST /api/v1/invitations/[REDACTED]/acceptances -> 201"
            )
            .contains("traceId=trace-123")
            .contains("route: /api/v1/invitations/{token}/acceptances")
            .contains("\n  REQUEST\n")
            .contains("\n    headers:\n")
            .contains("\n    body:\n")
            .contains("\n  RESPONSE\n")
            .contains("Authorization: [REDACTED]")
            .contains("User-Agent: earth-trip-test")
            .contains("\"email\" : \"traveler@example.com\"")
            .contains("\"displayName\" : \"Earth Traveler\"")
            .contains("traveler@example.com")
            .contains("Earth Traveler")
            .doesNotContain(
                "path-secret",
                "query-secret",
                "header-secret",
                "request-password",
                "request-token",
                "response-cookie-secret",
                "response-token",
                "Cf-Ray",
                "cloudflare-noise",
                "Set-Cookie"
            );
    }

    @Test
    void binary_response는_원문을_변경하지_않고_크기만_기록한다() throws Exception {
        ApiRequestResponseLoggingFilter filter = new ApiRequestResponseLoggingFilter(
            new ObjectMapper(),
            16
        );
        MockHttpServletRequest request = new MockHttpServletRequest(
            "GET",
            "/api/v1/files/export"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        byte[] binary = new byte[128];
        Arrays.fill(binary, (byte) 7);
        CapturedLogs logs = captureLogs();

        try {
            filter.doFilter(request, response, (wrappedRequest, wrappedResponse) -> {
                wrappedResponse.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                wrappedResponse.getOutputStream().write(binary);
            });
        } finally {
            logs.close();
        }

        assertThat(response.getContentAsByteArray()).containsExactly(binary);
        assertThat(logs.events()).hasSize(1);
        assertThat(logs.events().getFirst().getFormattedMessage())
            .contains("binary payload omitted")
            .contains("capturedBytes=16")
            .contains("bytes: 128");
    }

    @Test
    void 처리되지_않은_예외는_로그에_500으로_기록한다() {
        ApiRequestResponseLoggingFilter filter = new ApiRequestResponseLoggingFilter(
            new ObjectMapper(),
            4096
        );
        MockHttpServletRequest request = new MockHttpServletRequest(
            "GET",
            "/api/v1/places/search"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturedLogs logs = captureLogs();

        try {
            assertThatThrownBy(() -> filter.doFilter(
                request,
                response,
                (wrappedRequest, wrappedResponse) -> {
                    throw new ServletException("JSON conversion failed");
                }
            )).isInstanceOf(ServletException.class);
        } finally {
            logs.close();
        }

        assertThat(logs.events()).hasSize(1);
        ILoggingEvent event = logs.events().getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage())
            .startsWith("HTTP_EXCHANGE GET /api/v1/places/search -> 500")
            .contains("failure: jakarta.servlet.ServletException");
    }

    @Test
    void 클라이언트_오류는_WARN으로_기록한다() throws Exception {
        ApiRequestResponseLoggingFilter filter = new ApiRequestResponseLoggingFilter(
            new ObjectMapper(),
            4096
        );
        MockHttpServletRequest request = new MockHttpServletRequest(
            "GET",
            "/api/v1/missing"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturedLogs logs = captureLogs();

        try {
            filter.doFilter(request, response, (wrappedRequest, wrappedResponse) ->
                ((HttpServletResponse) wrappedResponse).setStatus(404)
            );
        } finally {
            logs.close();
        }

        assertThat(logs.events()).hasSize(1);
        ILoggingEvent event = logs.events().getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage())
            .startsWith("HTTP_EXCHANGE GET /api/v1/missing -> 404");
    }

    private static CapturedLogs captureLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(
            ApiRequestResponseLoggingFilter.class
        );
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);
        return new CapturedLogs(logger, appender, previousLevel);
    }

    private record CapturedLogs(
        Logger logger,
        ListAppender<ILoggingEvent> appender,
        Level previousLevel
    ) {
        private java.util.List<ILoggingEvent> events() {
            return appender.list;
        }

        private void close() {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
    }
}
