package com.earthtrip.platform.adapter.in.web.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
class RequestRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_SECONDS = 60;
    private static final List<Rule> RULES = List.of(
        rule("SHARE_PASSWORD", 8, request -> post(request)
            && request.getRequestURI().matches("/api/v1/shared-trips/[^/]+/password-verifications/?")),
        rule("SESSION_REFRESH", 120, request -> post(request)
            && request.getRequestURI().matches("/api/v1/auth/session-refreshes/?")),
        rule("PUBLIC_AUTH", 20, request -> post(request)
            && request.getRequestURI().startsWith("/api/v1/auth/")),
        rule("PUBLIC_INVITATION", 30, request -> post(request)
            && request.getRequestURI().startsWith("/api/v1/invitations/")),
        rule("PUBLIC_SHARE", 120, request -> request.getMethod().equals("GET")
            && request.getRequestURI().startsWith("/api/v1/shared-trips/")),
        rule("FILE_MUTATION", 120, request -> !request.getMethod().equals("GET")
            && request.getRequestURI().startsWith("/api/v1/files/"))
    );

    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong requestCount = new AtomicLong();

    RequestRateLimitFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        Rule rule = RULES.stream().filter(candidate -> candidate.matches().test(request))
            .findFirst().orElse(null);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long nowSeconds = clock.instant().getEpochSecond();
        long windowId = nowSeconds / WINDOW_SECONDS;
        String key = rule.id() + ':' + clientAddress(request);
        Window current = windows.compute(key, (ignored, previous) ->
            previous == null || previous.id() != windowId
                ? new Window(windowId, 1)
                : new Window(windowId, previous.count() + 1)
        );
        long resetAt = (windowId + 1) * WINDOW_SECONDS;
        response.setHeader("RateLimit-Limit", String.valueOf(rule.limit()));
        response.setHeader(
            "RateLimit-Remaining", String.valueOf(Math.max(0, rule.limit() - current.count()))
        );
        response.setHeader("RateLimit-Reset", String.valueOf(resetAt));
        cleanup(windowId);
        if (current.count() <= rule.limit()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(Math.max(1, resetAt - nowSeconds)));
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
            "{\"code\":\"RATE_LIMIT_EXCEEDED\","
                + "\"detail\":\"요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.\"}"
        );
    }

    private void cleanup(long activeWindowId) {
        if (requestCount.incrementAndGet() % 1_000 != 0) {
            return;
        }
        windows.entrySet().removeIf(entry -> entry.getValue().id() < activeWindowId - 1);
    }

    private static String clientAddress(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (isLoopback(remote)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",", 2)[0].strip();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.strip();
            }
        }
        return remote == null || remote.isBlank() ? "unknown" : remote;
    }

    private static boolean isLoopback(String address) {
        return "127.0.0.1".equals(address) || "0:0:0:0:0:0:0:1".equals(address)
            || "::1".equals(address);
    }

    private static boolean post(HttpServletRequest request) {
        return request.getMethod().equals("POST");
    }

    private static Rule rule(
        String id,
        int limit,
        Predicate<HttpServletRequest> matches
    ) {
        return new Rule(id, limit, matches);
    }

    private record Rule(String id, int limit, Predicate<HttpServletRequest> matches) { }

    private record Window(long id, int count) { }
}
