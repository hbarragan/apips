package com.adasoft.pharmasuite.apips.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
@DependsOn("apiConfig")
public class WebSocketConfig implements WebSocketConfigurer {
    private final WebSocketHandler handler;

    public WebSocketConfig(WebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws").setAllowedOrigins("*");
    }

}