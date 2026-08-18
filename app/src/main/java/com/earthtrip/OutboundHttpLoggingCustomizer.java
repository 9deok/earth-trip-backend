package com.earthtrip;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
final class OutboundHttpLoggingCustomizer implements RestClientCustomizer {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(OutboundHttpLoggingCustomizer.class);

    @Override
    public void customize(RestClient.Builder builder) {
        builder.requestInterceptor(new SafeOutboundLoggingInterceptor());
    }

    private static final class SafeOutboundLoggingInterceptor
            implements ClientHttpRequestInterceptor {

        private static final Pattern TOKEN_PATH =
                Pattern.compile("(?i)(/(?:uploads|downloads|invitations|shared-trips)/)[^/?#\\s]+");
        private static final Pattern PLACE_PHOTO_PATH = Pattern.compile("(?i)(/photos/)[^/?#\\s]+");

        @Override
        public ClientHttpResponse intercept(
                HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                throws IOException {
            long startedAt = System.nanoTime();
            String target = target(request.getURI());
            try {
                ClientHttpResponse response = execution.execute(request, body);
                long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
                LOGGER.info(
                        "OUTBOUND_HTTP {} {} -> {} ({}ms) requestBytes={}",
                        request.getMethod(),
                        target,
                        response.getStatusCode().value(),
                        durationMillis,
                        body.length);
                return response;
            } catch (IOException | RuntimeException exception) {
                long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
                LOGGER.error(
                        "OUTBOUND_HTTP {} {} -> FAILED ({}ms) requestBytes={} exception={}",
                        request.getMethod(),
                        target,
                        durationMillis,
                        body.length,
                        exception.getClass().getName(),
                        exception);
                throw exception;
            }
        }

        private static String target(URI uri) {
            String host = uri.getHost() == null ? "unknown-host" : safe(uri.getHost());
            String path = uri.getPath() == null || uri.getPath().isBlank() ? "/" : uri.getPath();
            path = TOKEN_PATH.matcher(path).replaceAll("$1[REDACTED]");
            path = PLACE_PHOTO_PATH.matcher(path).replaceAll("$1[REDACTED]");
            return host + safe(path);
        }

        private static String safe(String value) {
            return value.replaceAll("[\\r\\n\\t]", "_");
        }
    }
}
