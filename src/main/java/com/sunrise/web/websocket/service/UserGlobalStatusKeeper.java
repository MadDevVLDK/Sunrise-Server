package com.sunrise.web.websocket.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserGlobalStatusKeeper {

    @Value("${app.websocket.action-ttl-seconds:30}")
    private long actionTtlSec;

    private final Map<Long, String> userGlobalStatus = new ConcurrentHashMap<>(); // userId -> status
    
    private final Map<Long, Map<Long, Action>> userChatsActions = new ConcurrentHashMap<>(); // chatId -> Map<userId, status>
    private final Map<Long, Set<Long>> userChats = new ConcurrentHashMap<>(); // userId -> Set<chatId>

    private static class Action {
        final String value;
        final long timestamp;
        Action(String value) { this.value = value; this.timestamp = System.currentTimeMillis(); }
    }


    // ================= USER-GLOBAL-STATUS ==================

    public boolean updateUserStatus(long userId, String status) {
        String previousStatus = userGlobalStatus.put(userId, status);
        // Если статус не изменился (включая случай, когда previousStatus == null и status == null), не уведомляем
        if (previousStatus != null && previousStatus.equals(status)) {
            return false;
        }
        return true;
    }

    public Optional<String> getUserStatus(long userId) {
        return Optional.ofNullable(userGlobalStatus.get(userId));
    }


    // ==================== CHAT-ACTIONS ====================

    public boolean updateUserAction(long chatId, long userId, String action) {
        Map<Long, Action> actions = userChatsActions.computeIfAbsent(chatId, k -> new ConcurrentHashMap<>());
        userChats.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(chatId);
        // Если действие не изменилось, возвращаем false
        Action previous = actions.put(userId, new Action(action));
        return previous == null || !previous.value.equals(action);
    }

    public Map<Long, String> getChatActions(long chatId) {
        Map<Long, Action> actions = userChatsActions.get(chatId);
        if (actions == null) {
            return Collections.emptyMap();
        }

        long now = System.currentTimeMillis();
        Map<Long, String> result = new HashMap<>();
        Iterator<Map.Entry<Long, Action>> iterator = actions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Action> entry = iterator.next();
            if (now - entry.getValue().timestamp > actionTtlSec * 1000) {
                iterator.remove();   // удаляем устаревшее
            } else {
                result.put(entry.getKey(), entry.getValue().value);
            }
        }

        if (actions.isEmpty()) {
            userChatsActions.remove(chatId);
        }
        return result;
    }

    // Очистка действий пользователя в конкретном чате
    public void removeUserActionFromChat(long chatId, long userId) {
        Map<Long, Action> actions = userChatsActions.get(chatId);
        if (actions != null) {
            actions.remove(userId);
            if (actions.isEmpty()) userChatsActions.remove(chatId);
        }
        Set<Long> chats = userChats.get(userId);
        if (chats != null) {
            chats.remove(chatId);
            if (chats.isEmpty()) userChats.remove(userId);
        }
    }

    // Очистка всех действий пользователя
    public void removeUserActions(long userId) {
        Set<Long> chats = userChats.remove(userId);
        if (chats != null) {
            for (long chatId : chats) {
                Map<Long, Action> actions = userChatsActions.get(chatId);
                if (actions != null) {
                    actions.remove(userId);
                    if (actions.isEmpty()) userChatsActions.remove(chatId);
                }
            }
        }
    }

    // Очистка всего чата
    public void removeChatActions(long chatId) {
        Map<Long, Action> actions = userChatsActions.remove(chatId);
        if (actions != null) {
            for (Long userId : actions.keySet()) {
                Set<Long> chats = userChats.get(userId);
                if (chats != null) {
                    chats.remove(chatId);
                    if (chats.isEmpty()) userChats.remove(userId);
                }
            }
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    public void cleanExpiredActions() {
        long now = System.currentTimeMillis();
        userChatsActions.entrySet().removeIf(entry -> {
            Map<Long, Action> actions = entry.getValue();
            actions.entrySet().removeIf(e -> now - e.getValue().timestamp > actionTtlSec * 1000);
            return actions.isEmpty();
        });
        userChats.entrySet().removeIf(entry -> {
            long userId = entry.getKey();
            Set<Long> chats = entry.getValue();
            if (chats == null) return true;
            chats.removeIf(chatId -> {
                Map<Long, Action> actions = userChatsActions.get(chatId);
                return actions == null || !actions.containsKey(userId);
            });
            return chats.isEmpty();
        });
    }
}