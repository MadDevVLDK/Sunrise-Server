package com.sunrise.web.websocket.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.sunrise.orchestrator.service.ChatMemberOrchestrator;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsSubscriptionInterceptor implements ChannelInterceptor {

    private final ChatMemberOrchestrator chatMemberOrchestrator;


    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            
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
            
            if (userId == null) {
                log.warn("[🔒] Subscription denied: userId not found in session");
                return createErrorMessage(accessor, "Not authenticated");
            }

            if (destination != null && destination.startsWith("/topic/chat/")) {
                try {
                    long chatId = Long.parseLong(destination.substring("/topic/chat/".length()));
                    if (!chatMemberOrchestrator.hasActive(userId, chatId)) {
                        log.warn("[🔒] User {} tried to subscribe to chat {} without membership", userId, chatId);
                        return createErrorMessage(accessor, "Access denied to this chat");
                    }
                } catch (NumberFormatException e) {
                    log.warn("[🔒] NumberFormatException on parsing chatId websocket {}", userId);
                    return createErrorMessage(accessor, "Invalid chat id");
                }
            }
        }
        return message;
    }

    private Message<?> createErrorMessage(StompHeaderAccessor accessor, String errorMessage) {
        log.error("[🔒] STOMP subscription error: {}", errorMessage);
        StompHeaderAccessor errorAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
        errorAccessor.setMessage(errorMessage);
        errorAccessor.setSessionId(accessor.getSessionId());
        return MessageBuilder.createMessage(new byte[0], errorAccessor.getMessageHeaders());
    }
}
