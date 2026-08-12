package com.earthtrip;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration(proxyBeanMethods = false)
class OutboundHttpClientConfiguration {

    @Bean
    RestClientCustomizer outboundHttpTimeoutCustomizer(
            @Value("${earthtrip.http-client.connect-timeout:5s}") Duration connectTimeout,
            @Value("${earthtrip.http-client.read-timeout:15s}") Duration readTimeout) {
        if (connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException("HTTP connect timeout must be positive");
        }
        if (readTimeout.isNegative() || readTimeout.isZero()) {
            throw new IllegalArgumentException("HTTP read timeout must be positive");
        }
        HttpClient client =
                HttpClient.newBuilder()
                        .connectTimeout(connectTimeout)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(client);
        requestFactory.setReadTimeout(readTimeout);
        return builder -> builder.requestFactory(requestFactory);
    }
}
