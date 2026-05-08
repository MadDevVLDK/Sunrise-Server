package com.sunrise.web.websocket.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
public class WsHandshakeHandler extends DefaultHandshakeHandler {
    
    @Override
    protected Principal determineUser(@NonNull ServerHttpRequest request, @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        Long userId = (Long) attributes.get("userId");
        return userId != null ? () -> userId.toString() : null;
    }
}
