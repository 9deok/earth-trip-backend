package com.earthtrip.identity.adapter.in.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

final class InternalTokenFilter extends OncePerRequestFilter {

    static final String TOKEN_HEADER = "X-EarthTrip-Internal-Token";

    private final byte[] configuredToken;

    InternalTokenFilter(String configuredToken) {
        this.configuredToken = configuredToken == null
            ? new byte[0]
            : configuredToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/internal/admin/") || path.equals("/actuator/prometheus"));
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (configuredToken.length == 0) {
            writeProblem(
                response,
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "INTERNAL_AUTH_NOT_CONFIGURED",
                "내부 운영 인증 토큰이 설정되지 않았습니다."
            );
            return;
        }
        String supplied = request.getHeader(TOKEN_HEADER);
        byte[] suppliedBytes = supplied == null
            ? new byte[0]
            : supplied.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(configuredToken, suppliedBytes)) {
            writeProblem(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "INVALID_INTERNAL_TOKEN",
                "올바른 내부 운영 인증 토큰이 필요합니다."
            );
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static void writeProblem(
        HttpServletResponse response,
        int status,
        String code,
        String detail
    ) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(
            "{\"title\":\"Internal request rejected\",\"status\":" + status
                + ",\"detail\":\"" + detail + "\",\"code\":\"" + code + "\"}"
        );
    }
}
