package com.earthtrip.platform.adapter.in.realtime;

import java.util.Arrays;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration(proxyBeanMethods = false)
@EnableWebSocket
class TripRealtimeWebSocketConfiguration implements WebSocketConfigurer {

    private final TripRealtimeWebSocketHandler handler;
    private final Environment environment;

    TripRealtimeWebSocketConfiguration(
        TripRealtimeWebSocketHandler handler,
        Environment environment
    ) {
        this.handler = handler;
        this.environment = environment;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        var registration = registry.addHandler(handler, "/ws/v1/trips/{tripId}");
        String configured = environment.getProperty("earthtrip.realtime.allowed-origins", "");
        String[] origins = Arrays.stream(configured.split(","))
            .map(String::strip)
            .filter(origin -> !origin.isEmpty())
            .toArray(String[]::new);
        if (origins.length > 0) {
            registration.setAllowedOrigins(origins);
        }
    }
}
