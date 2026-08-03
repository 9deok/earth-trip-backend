package com.earthtrip.identity.adapter.in.security;

import com.earthtrip.identity.application.port.in.AccessTokenAuthenticationUseCase;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
class IdentitySecurityConfiguration {

    @Bean
    BearerTokenFilter bearerTokenFilter(
        AccessTokenAuthenticationUseCase authenticationUseCase
    ) {
        return new BearerTokenFilter(authenticationUseCase);
    }

    @Bean
    InternalTokenFilter internalTokenFilter(
        org.springframework.core.env.Environment environment
    ) {
        return new InternalTokenFilter(environment.getProperty(
            "earthtrip.internal.admin-token",
            ""
        ));
    }

    @Bean
    SecurityFilterChain earthTripSecurityFilterChain(
        HttpSecurity http,
        BearerTokenFilter bearerTokenFilter,
        InternalTokenFilter internalTokenFilter
    ) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health/**", "/error").permitAll()
                .requestMatchers("/internal/**", "/actuator/prometheus").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/app-capabilities").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/policies/current").permitAll()
                .requestMatchers("/api/v1/auth/**", "/api/v1/invitations/**", "/api/v1/shared-trips/**")
                    .permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/problem+json;charset=UTF-8");
                response.getWriter().write(
                    "{\"title\":\"Unauthorized\",\"status\":401,"
                        + "\"detail\":\"로그인이 필요합니다.\","
                        + "\"code\":\"AUTHENTICATION_REQUIRED\"}"
                );
            }))
            .addFilterBefore(internalTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(bearerTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
