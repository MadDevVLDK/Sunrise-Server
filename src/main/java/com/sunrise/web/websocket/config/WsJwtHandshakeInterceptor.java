package com.sunrise.web.websocket.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.sunrise.helpclass.JwtUtil;
import com.sunrise.orchestrator.service.UserOrchestrator;

import java.util.Map;
import java.util.Optional;


@Slf4j
@RequiredArgsConstructor
@Component
public class WsJwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final UserOrchestrator userOrchestrator;

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, 
                                   @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {

        String query = request.getURI().getQuery();
        String token = null;

        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    token = param.substring(6);
                    break;
                }
            }
        }

        if (token != null && jwtUtil.validateToken(token)) {
            long userId = jwtUtil.extractUserId(token);
            Integer tokenVersion = jwtUtil.extractJwtVersion(token);
            try {
                Optional<Integer> version = userOrchestrator.getUserJwtVersion(userId);
                if (tokenVersion != null && version.isPresent() && tokenVersion.equals(version.get())) {
                    attributes.put("userId", userId);
                    log.info("[🔐] ✅ WebSocket handshake successful: userId={}", userId);
                    return true;
                } else {
                    log.warn("[🔐] ❌ WebSocket handshake failed: JWT version mismatch for userId={}", userId);
                }
            } catch (Exception e) {
                log.error("[🔐] ❌ Error validating JWT version for user: {}", userId, e);
                return false;
            }
        } else {
            log.warn("[🔐] ❌ WebSocket handshake failed: invalid or missing token");
        }
        return false;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, 
                               @NonNull WebSocketHandler wsHandler, @Nullable Exception exception) {

        // ...existing code...
    }
}
