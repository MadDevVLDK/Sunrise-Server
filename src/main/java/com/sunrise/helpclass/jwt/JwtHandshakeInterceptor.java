package com.sunrise.helpclass.jwt;

import com.sunrise.dataservice.DataOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final DataOrchestrator dataOrchestrator;

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
                Optional<Integer> version = dataOrchestrator.getUserJwtVersion(userId);
                if (tokenVersion != null && version.isPresent() && tokenVersion.equals(version.get())) {
                    String sessionId = UUID.randomUUID().toString();
                    attributes.put("userId", userId);
                    attributes.put("sessionId", sessionId);
                    log.info("[🔐] ✅ WebSocket handshake successful: userId={}, sessionId={}", userId, sessionId);
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
