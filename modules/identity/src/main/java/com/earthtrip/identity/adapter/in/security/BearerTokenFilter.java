package com.earthtrip.identity.adapter.in.security;

import com.earthtrip.identity.application.port.in.AccessTokenAuthenticationUseCase;
import com.earthtrip.identity.application.port.in.AccessTokenAuthenticationUseCase.AuthenticationResult;
import com.earthtrip.sharedkernel.error.EarthTripException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

class BearerTokenFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";
    private final AccessTokenAuthenticationUseCase authenticationUseCase;

    BearerTokenFilter(AccessTokenAuthenticationUseCase authenticationUseCase) {
        this.authenticationUseCase = authenticationUseCase;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            AuthenticationResult result =
                    authenticationUseCase.authenticate(
                            authorization.substring(PREFIX.length()).strip());
            EarthTripPrincipal principal =
                    new EarthTripPrincipal(
                            result.userId(), result.sessionId(), result.displayName());
            SecurityContextHolder.getContext()
                    .setAuthentication(
                            new UsernamePasswordAuthenticationToken(principal, null, List.of()));
            filterChain.doFilter(request, response);
        } catch (EarthTripException exception) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(response, request, exception);
        }
    }

    private static void writeUnauthorized(
            HttpServletResponse response, HttpServletRequest request, EarthTripException exception)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String traceId =
                request.getAttribute("earthTripTraceId") instanceof String value
                        ? value
                        : "unknown";
        response.getWriter()
                .write(
                        "{\"type\":\"https://earthtrip.app/problems/invalid-access-token\","
                                + "\"title\":\"Unauthorized\",\"status\":401,"
                                + "\"detail\":\"로그인 세션이 만료되었거나 올바르지 않습니다.\","
                                + "\"code\":\""
                                + exception.code()
                                + "\","
                                + "\"traceId\":\""
                                + traceId
                                + "\"}");
    }
}
