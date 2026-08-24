package com.sunrise.web.websocket.config;

import com.sunrise.web.websocket.service.SessionRegistry;
import com.sunrise.web.websocket.service.UserGlobalStatusKeeper;
import com.sunrise.web.websocket.service.WebSocketNotifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;


@Slf4j
@RequiredArgsConstructor
@Component
public class WsEventListener {

    private final SessionRegistry sessionRegistry;
    private final UserGlobalStatusKeeper userGlobalStatusKeeper;
    private final WebSocketNotifier wsNotify;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
    
        Long userId = null;
        if (sessionAttributes != null) {
            Object userIdObj = sessionAttributes.get("userId");
            if (userIdObj instanceof Long) {
                userId = (Long) userIdObj;
            } else if (userIdObj instanceof String) {
                try {
                    userId = Long.parseLong((String) userIdObj);
                } catch (NumberFormatException ignored) {}
            }
        }

        if (userId != null && sessionId != null) {
            sessionRegistry.register(sessionId, userId);
            log.info("[🗝️] ✅ WebSocket connected: sessionId={}, userId={}", sessionId, userId);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        Long userId = sessionRegistry.getUserId(sessionId);

        if (userId != null) {
            boolean noMoreSessions = !sessionRegistry.userHasSessions(userId);
            sessionRegistry.unregister(sessionId);
            if (noMoreSessions && userGlobalStatusKeeper.updateUserStatus(userId, "offline")) {
                wsNotify.notifyUserStatus(userId, "offline");
                userGlobalStatusKeeper.removeUserActions(userId);  // только если пользователь полностью оффлайн
            }
            log.info("[🗝️] ❌ WebSocket disconnected: sessionId={}, userId={}", sessionId, userId);
        }
    }
}
