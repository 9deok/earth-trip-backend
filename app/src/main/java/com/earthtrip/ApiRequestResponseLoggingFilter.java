package com.earthtrip;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
final class ApiRequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ApiRequestResponseLoggingFilter.class);
    private static final String TRACE_ID_ATTRIBUTE = "earthTripTraceId";

    private final HttpLogSanitizer sanitizer;
    private final int maxPayloadBytes;
    private final boolean successDetailsEnabled;

    ApiRequestResponseLoggingFilter(
            ObjectMapper objectMapper,
            @Value("${earthtrip.logging.http.max-payload-bytes:8192}") int maxPayloadBytes,
            @Value("${earthtrip.logging.http.success-details-enabled:true}")
                    boolean successDetailsEnabled) {
        if (maxPayloadBytes < 1) {
            throw new IllegalArgumentException(
                    "earthtrip.logging.http.max-payload-bytes must be positive");
        }
        this.sanitizer = new HttpLogSanitizer(objectMapper);
        this.maxPayloadBytes = maxPayloadBytes;
        this.successDetailsEnabled = successDetailsEnabled;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        LimitedContentCachingRequestWrapper requestWrapper =
                new LimitedContentCachingRequestWrapper(request, maxPayloadBytes);
        CapturingHttpServletResponseWrapper responseWrapper =
                new CapturingHttpServletResponseWrapper(
                        response, maxPayloadBytes, successDetailsEnabled);
        long startedAt = System.nanoTime();
        boolean completed = false;
        Throwable failure = null;

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
            completed = true;
        } catch (ServletException | IOException exception) {
            failure = exception;
            throw exception;
        } catch (RuntimeException | Error exception) {
            failure = exception;
            throw exception;
        } finally {
            try {
                if (completed) {
                    responseWrapper.flushCapturedWriter();
                }
            } finally {
                writeExchangeLog(requestWrapper, responseWrapper, startedAt, failure);
            }
        }
    }

    private void writeExchangeLog(
            LimitedContentCachingRequestWrapper request,
            CapturingHttpServletResponseWrapper response,
            long startedAt,
            Throwable failure) {
        try {
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            Object routeAttribute =
                    request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String route = routeAttribute == null ? "-" : routeAttribute.toString();
            String traceId =
                    request.getAttribute(TRACE_ID_ATTRIBUTE) instanceof String value
                            ? value
                            : "unknown";
            int status =
                    failure != null && response.getStatus() < 400
                            ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                            : response.getStatus();

            if (status < 400 && !successDetailsEnabled) {
                log(
                        status,
                        "HTTP_EXCHANGE %s %s -> %d (%dms) responseBytes=%d traceId=%s"
                                .formatted(
                                        request.getMethod(),
                                        sanitizer.path(request.getRequestURI()),
                                        status,
                                        durationMillis,
                                        response.getTotalBytes(),
                                        sanitizer.oneLine(traceId)));
                return;
            }

            String message =
                    """
                HTTP_EXCHANGE %s %s -> %d (%dms) traceId=%s
                  route: %s
                  failure: %s
                  REQUEST
                    query: %s
                    headers:
                %s
                    body:
                %s
                    declaredBytes: %d
                  RESPONSE
                    headers:
                %s
                    body:
                %s
                    bytes: %d
                """
                            .formatted(
                                    request.getMethod(),
                                    sanitizer.path(request.getRequestURI()),
                                    status,
                                    durationMillis,
                                    sanitizer.oneLine(traceId),
                                    sanitizer.oneLine(route),
                                    failure == null ? "-" : failure.getClass().getName(),
                                    sanitizer.query(request.getQueryString()),
                                    indent(sanitizer.requestHeaders(request), 6),
                                    indent(
                                            sanitizer.payload(
                                                    request.getContentAsByteArray(),
                                                    request.getContentType(),
                                                    request.getCharacterEncoding(),
                                                    request.isOverflowed(),
                                                    request.getContentLengthLong()),
                                            6),
                                    request.getContentLengthLong(),
                                    indent(sanitizer.responseHeaders(response), 6),
                                    indent(
                                            sanitizer.payload(
                                                    response.getCapturedContent(),
                                                    response.getContentType(),
                                                    response.getCharacterEncoding(),
                                                    response.isOverflowed(),
                                                    response.getTotalBytes()),
                                            6),
                                    response.getTotalBytes());
            log(status, message.stripTrailing());
        } catch (RuntimeException exception) {
            LOGGER.warn("HTTP exchange log creation failed", exception);
        }
    }

    private static void log(int status, String message) {
        if (status >= 500) {
            LOGGER.error(message);
        } else if (status >= 400) {
            LOGGER.warn(message);
        } else {
            LOGGER.info(message);
        }
    }

    private static String indent(String value, int spaces) {
        String prefix = " ".repeat(spaces);
        return value.lines().map(prefix::concat).collect(java.util.stream.Collectors.joining("\n"));
    }

    private static final class LimitedContentCachingRequestWrapper
            extends ContentCachingRequestWrapper {

        private boolean overflowed;

        private LimitedContentCachingRequestWrapper(HttpServletRequest request, int cacheLimit) {
            super(request, cacheLimit);
        }

        @Override
        protected void handleContentOverflow(int contentCacheLimit) {
            overflowed = true;
        }

        private boolean isOverflowed() {
            return overflowed;
        }
    }
}
