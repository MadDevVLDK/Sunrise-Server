package com.sunrise.web.websocket.service;

import com.sunrise.db.service.EventDbService.ChatEvent;
import com.sunrise.db.service.EventDbService.UserEvent;
import com.sunrise.helpclass.exception.MyException;
import com.sunrise.orchestrator.event.EventRegistry;
import com.sunrise.orchestrator.event.IDomainEvent;
import com.sunrise.orchestrator.result.Dto.GlobalEvent;
import com.sunrise.web.payload.WsResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotifier {

    private final SimpMessagingTemplate messagingTemplate;


    // ========================= EVENTS ===========================

    @Async("webSocketNotifierExecutor")
    public void notifyUserEvent(UserEvent userEvent, IDomainEvent payload) {
        try {
            messagingTemplate.convertAndSendToUser(
                userEvent.userId() + "", 
                "/queue/user-events", 
                new GlobalEvent(userEvent.eventId(), EventRegistry.getEventType(payload), payload, payload.getCreatedAt())
            );
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to send user events to userId={}: {}", userEvent.userId(), e.getMessage(), e);
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyChatEvent(ChatEvent event, IDomainEvent payload) {
        try {
            messagingTemplate.convertAndSend(
                "/topic/chats/" + event.chatId(), 
                new GlobalEvent(event.eventId(), EventRegistry.getEventType(payload), payload, payload.getCreatedAt())
            );
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to send to chat topic chatId={}: {}", event.chatId(), e.getMessage(), e);
        }
    }


    // ================= PRESENCE/STATUS/OTHER ====================

    @Async("webSocketNotifierExecutor")
    public void notifyUserStatusToSubscriber(long userId, String sessionId, long actorUserId, String newStatus) {
        sendToUserSessionQueue(userId, sessionId, "/user-statuses", new WsResponse.UserStatus(actorUserId, newStatus));
        log.debug("[💬] 🔔 Sent initial user status: target={} status={} to subscriber {} session={}", actorUserId, newStatus, userId, sessionId);
    }

    @Async("webSocketNotifierExecutor")
    public void notifyUserStatus(long actorUserId, String newStatus) {
        try{
            messagingTemplate.convertAndSend(
                "/topic/users/" + actorUserId + "/statuses", 
                new WsResponse.UserStatus(actorUserId, newStatus)
            );
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to send to user global status topic userId={}: {}", actorUserId, e.getMessage(), e);
        }
    }


    @Async("webSocketNotifierExecutor")
    public void notifyUserChatActionToSubscriber(long userId, String sessionId, long chatId, long actorUserId, String action) {
        sendToUserSessionQueue(userId, sessionId, "/chat-actions", new WsResponse.UserChatAction(actorUserId, chatId, action));
        log.debug("[💬] 🔔 Sent initial chat action: user={} action='{}' in chat={} to subscriber {} session={}", actorUserId, action, chatId, userId, sessionId);
    }

    @Async("webSocketNotifierExecutor")
    public void notifyUserChatAction(long chatId, long userId, String action) {
        try {
            messagingTemplate.convertAndSend(
                "/topic/chats/" + chatId + "/actions", 
                new WsResponse.UserChatAction(userId, chatId, action)
            );
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to send to chat actions topic chatId={}: {}", chatId, e.getMessage(), e);
        }
    }


    @Async("webSocketNotifierExecutor")
    public void notifyPong(long userId, @NonNull String sessionId) {
        sendToUserSessionQueue(userId, sessionId, "/pong", new WsResponse.Pong());
        log.debug("[💬] 🏓 Sent pong to session {}", sessionId);
    }

    @Async("webSocketNotifierExecutor")
    public void notifyError(long userId, @NonNull String sessionId, MyException exception, String errorUrl) {
        sendToUserSessionQueue(userId, sessionId, "/errors",
            new WsResponse.Error(exception.getCode().name(), exception.getMessage(), errorUrl));
        log.debug("[💬] ⚠️ Sent error to session {}: code={}, msg={}", sessionId, exception.getCode(), exception.getMessage());
    }

    @Async("webSocketNotifierExecutor")
    public void notifyError(long userId, @NonNull String sessionId, String message, String errorUrl) {
        sendToUserSessionQueue(userId, sessionId, "/errors",
            new WsResponse.Error("VALIDATION_ERROR", message, errorUrl));
        log.debug("[💬] ⚠️ Sent error to session {}: {}", sessionId, message);
    }


    // ======================= PRIVATE ============================

    private void sendToUserSessionQueue(long userId, @NonNull String sessionId, String path, @NonNull Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(
                userId + "", 
                "/queue" + path, 
                payload,
                Map.of("simpSessionId", sessionId)
            );
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to send to user session queue: userId={}, sessionId={}, path={}, error={}", userId, sessionId, path, e.getMessage(), e);
        }
    }
}