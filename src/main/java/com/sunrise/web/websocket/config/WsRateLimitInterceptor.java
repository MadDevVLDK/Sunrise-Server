package com.sunrise.web.websocket.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Component
@RequiredArgsConstructor
public class WsRateLimitInterceptor implements ChannelInterceptor {
    
    private final WsRateLimitProperties properties;

    // "userId:commandType", значение = счётчик запросов за текущую минуту
    private final Cache<String, AtomicInteger> rateLimitCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(100_000)
            .build();

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        if (!properties.isEnabled()) {
            return message;
        }

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (!StompCommand.SEND.equals(accessor.getCommand())) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return message;
        }

        Long userId = extractUserId(accessor);
        if (userId == null) {
            log.warn("Rate limiting skipped: userId not found in session, destination={}", destination);
            return message;
        }

        String commandType = resolveCommandType(destination);
        if (commandType == null) {
            commandType = "unknown";
        }

        int limit = properties.getCommands().getOrDefault(commandType, properties.getDefaultLimit());
        if (limit <= 0) {
            // Лимит не установлен или 0 – не ограничиваем
            return message;
        }

        String key = userId + ":" + commandType;
        AtomicInteger counter = rateLimitCache.get(key, k -> new AtomicInteger(0));
        
        int current = counter.incrementAndGet();
        if (current > limit) {
            log.warn("[RateLimit] Blocked userId={}, command={}, count={}, limit={}", userId, commandType, current, limit);
            return null; // блокируем, клиент не получает ответа
        }
        return message;
    }

    private Long extractUserId(StompHeaderAccessor accessor) {
        Object userIdObj = accessor.getSessionAttributes().get("userId");

        if (userIdObj instanceof Long) return (Long) userIdObj;
        if (userIdObj instanceof String) {
            try {
                return Long.parseLong((String) userIdObj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String resolveCommandType(String destination) {
        // Порядок важен: сначала более специфичные паттерны
        if (destination.matches("^/app/chats/\\d+/messages/send$")) {
            return "send_message";
        }
        if (destination.matches("^/app/chats/\\d+/messages/\\d+/edit$")) {
            return "edit_message";
        }
        if (destination.matches("^/app/chats/\\d+/messages/\\d+/delete$")) {
            return "delete_message";
        }
        if (destination.matches("^/app/chats/\\d+/messages/\\d+/up-to-read$")) {
            return "mark_read";
        }
        if (destination.matches("^/app/chats/\\d+/actions/\\w+$")) {
            return "user_action";
        }
        if (destination.equals("/app/ping")) {
            return "ping";
        }
        if (destination.startsWith("/app/subscribe/") || destination.startsWith("/app/unsubscribe/")) {
            return "subscription";
        }
        return null;
    }
}
