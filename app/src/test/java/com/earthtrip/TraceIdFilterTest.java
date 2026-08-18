package com.earthtrip;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

    @Test
    void 요청_처리_중_MDC에_traceId를_제공하고_완료_후_정리한다() throws Exception {
        TraceIdFilter filter = new TraceIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me");
        request.addHeader("X-Trace-Id", "mobile.trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (wrappedRequest, wrappedResponse) ->
                        assertThat(MDC.get("traceId")).isEqualTo("mobile.trace-123"));

        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("mobile.trace-123");
        assertThat(MDC.get("traceId")).isNull();
    }
}
