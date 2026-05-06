package com.sunrise.web.websocket.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.sunrise.orchestrator.service.ChatMemberOrchestrator;
import com.sunrise.web.payload.WsResponse;
import com.sunrise.web.websocket.service.UserGlobalStatusKeeper;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class WsSubscriptionInterceptor implements ChannelInterceptor {

    private final ChatMemberOrchestrator chatMemberOrchestrator;
    private final UserGlobalStatusKeeper statusKeeper;
    private final SimpMessagingTemplate messagingTemplate;

    private static final Pattern USER_STATUS_PATTERN = Pattern.compile("^/topic/users/(\\d+)/statuses$");
    private static final Pattern CHAT_ACTIONS_PATTERN = Pattern.compile("^/topic/chats/(\\d+)/actions$");
    private static final Pattern CHAT_TOPIC_PATTERN = Pattern.compile("^/topic/chat/(\\d+)$");

    public WsSubscriptionInterceptor(ChatMemberOrchestrator chatMemberOrchestrator, UserGlobalStatusKeeper statusKeeper,
                                     @Lazy SimpMessagingTemplate messagingTemplate) {

        this.chatMemberOrchestrator = chatMemberOrchestrator;
        this.statusKeeper = statusKeeper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

            Long tempUserId = null;
            if (sessionAttributes != null) {
                Object userIdObj = sessionAttributes.get("userId");
                if (userIdObj instanceof Long) {
                    tempUserId = (Long) userIdObj;
                } else if (userIdObj instanceof String) {
                    try {
                        tempUserId = Long.parseLong((String) userIdObj);
                    } catch (NumberFormatException ignored) {}
                }
            }
            
            if (tempUserId == null) {
                log.warn("[🔒] Subscription denied: userId not found in session");
                return createErrorMessage(accessor, "Not authenticated");
            }

            final long userId = tempUserId;
            final String sessionId = accessor.getSessionId();

            // логика для подписки на глобальный статус пользователя
            Matcher userStatusMatcher = USER_STATUS_PATTERN.matcher(destination);
            if (userStatusMatcher.matches()) {
                final long targetUserId = Long.parseLong(userStatusMatcher.group(1));
                
                statusKeeper.getUserStatus(targetUserId)
                    .ifPresent(status -> sendCurrentUserStatus(userId, sessionId, targetUserId, status));
                
                log.debug("[📡] User {} subscribed to status of user {}, sessionId={}", userId, targetUserId, sessionId);
                return message;
            }

            // логика для подписки на действия в чате
            Matcher chatActionsMatcher = CHAT_ACTIONS_PATTERN.matcher(destination);
            if (chatActionsMatcher.matches()) {
                final long chatId = Long.parseLong(chatActionsMatcher.group(1));
                if (!chatMemberOrchestrator.hasActive(userId, chatId)) {
                    log.warn("[🔒] User {} tried to subscribe to chat actions without membership, chatId={}", userId, chatId);
                    return createErrorMessage(accessor, "Access denied to this chat");
                }

                // отправляем текущие действия в личную очередь подписчика
                sendCurrentChatActions(userId, sessionId, chatId);
                log.debug("[📡] User {} subscribed to actions of chat {}, sessionId={}", userId, chatId, sessionId);
                return message;
            }

            // логика для подписки на чат
            Matcher chatTopicMatcher = CHAT_TOPIC_PATTERN.matcher(destination);
            if (chatTopicMatcher.matches()) {
                final long chatId = Long.parseLong(chatTopicMatcher.group(1));
                if (!chatMemberOrchestrator.hasActive(userId, chatId)) {
                    log.warn("[🔒] User {} tried to subscribe to chat {} without membership", userId, chatId);
                    return createErrorMessage(accessor, "Access denied to this chat");
                }

                log.debug("[📡] User {} subscribed to chat {}, sessionId={}", userId, chatId, sessionId);
                return message;
            }
        }
        return message;
    }

    private void sendCurrentChatActions(long userId, String sessionId, long chatId) {
        Map<Long, String> actions = statusKeeper.getChatActions(chatId);
        for (Map.Entry<Long, String> entry : actions.entrySet()) {
            long otherUserId = entry.getKey();
            String action = entry.getValue();
            messagingTemplate.convertAndSendToUser(
                userId + "", "/queue/chat-actions", 
                new WsResponse.UserChatAction(chatId, otherUserId, action), 
                Map.of("simpSessionId", sessionId)
            );
        }
    }

    private void sendCurrentUserStatus(long userId, String sessionId, long targetUserId, String status) {
        messagingTemplate.convertAndSendToUser(
            userId + "", "/queue/user-statuses", 
            new WsResponse.UserStatus(targetUserId, status), 
            Map.of("simpSessionId", sessionId)
        );
    }

    private Message<?> createErrorMessage(StompHeaderAccessor accessor, String errorMessage) {
        log.error("[🔒] STOMP subscription error: {}", errorMessage);
        StompHeaderAccessor errorAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
        errorAccessor.setMessage(errorMessage);
        errorAccessor.setSessionId(accessor.getSessionId());
        return MessageBuilder.createMessage(new byte[0], errorAccessor.getMessageHeaders());
    }
}
