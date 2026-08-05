package com.earthtrip.platform.adapter.in.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestRateLimitFilterTest {

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-08-04T12:00:00Z"), ZoneOffset.UTC
    );

    @Test
    void blocksRepeatedSharePasswordAttemptsPerClient() throws Exception {
        RequestRateLimitFilter filter = new RequestRateLimitFilter(CLOCK);
        AtomicInteger accepted = new AtomicInteger();
        MockHttpServletResponse last = null;

        for (int index = 0; index < 9; index++) {
            MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/v1/shared-trips/token/password-verifications"
            );
            request.setRemoteAddr("203.0.113.20");
            last = new MockHttpServletResponse();
            filter.doFilter(request, last, (ignoredRequest, ignoredResponse) ->
                accepted.incrementAndGet()
            );
        }

        assertThat(accepted).hasValue(8);
        assertThat(last).isNotNull();
        assertThat(last.getStatus()).isEqualTo(429);
        assertThat(last.getHeader("Retry-After")).isNotBlank();
        assertThat(last.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void keepsBucketsSeparatedByForwardedClientBehindLocalProxy() throws Exception {
        RequestRateLimitFilter filter = new RequestRateLimitFilter(CLOCK);
        AtomicInteger accepted = new AtomicInteger();

        for (int index = 0; index < 9; index++) {
            MockHttpServletRequest first = request("198.51.100.1");
            filter.doFilter(first, new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) -> accepted.incrementAndGet());
        }
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(request("198.51.100.2"), secondResponse,
            (ignoredRequest, ignoredResponse) -> accepted.incrementAndGet());

        assertThat(accepted).hasValue(9);
        assertThat(secondResponse.getStatus()).isEqualTo(200);
    }

    private static MockHttpServletRequest request(String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest(
            "POST", "/api/v1/shared-trips/token/password-verifications"
        );
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", forwardedFor);
        return request;
    }
}
