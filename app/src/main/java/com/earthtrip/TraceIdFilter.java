package com.earthtrip;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = normalize(request.getHeader("X-Trace-Id"));
        request.setAttribute("earthTripTraceId", traceId);
        response.setHeader("X-Trace-Id", traceId);
        MDC.put("traceId", traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
        }
    }

    private static String normalize(String candidate) {
        if (candidate == null || candidate.isBlank() || candidate.length() > 100) {
            return UUID.randomUUID().toString();
        }
        return candidate.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
