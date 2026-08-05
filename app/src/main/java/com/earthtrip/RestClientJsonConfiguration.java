package com.earthtrip;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration(proxyBeanMethods = false)
class RestClientJsonConfiguration {

    @Bean
    @SuppressWarnings("removal")
    ClientHttpMessageConvertersCustomizer jackson2RestClientMessageConverter(
        ObjectMapper objectMapper
    ) {
        return converters -> converters.withJsonConverter(
            new MappingJackson2HttpMessageConverter(objectMapper)
        );
    }
}
