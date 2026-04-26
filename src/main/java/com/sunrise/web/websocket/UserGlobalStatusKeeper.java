package com.sunrise.web.websocket;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserGlobalStatusKeeper {

    private final Map<Long, Set<String>> watchers = new ConcurrentHashMap<>(); // userId -> Set<sessionId>
    private final Map<Long, String> userOnlineStatus = new ConcurrentHashMap<>(); // userId -> status
    private final Map<String, String> userChatActions = new ConcurrentHashMap<>(); // "chatId:userId" -> status


    // ==================== Онлайн статусы ====================

    public Set<String> updateUserGlobalStatus(long userId, String status) {
        String previousStatus = userOnlineStatus.put(userId, status);
        // Если статус не изменился (включая случай, когда previousStatus == null и status == null), не уведомляем
        if (previousStatus != null && previousStatus.equals(status)) {
            return Collections.emptySet();
        }
        return Set.copyOf(watchers.getOrDefault(userId, Collections.emptySet()));
    }

    public void subscribeUserGlobalStatus(long targetUserId, String sessionId) {
        watchers.computeIfAbsent(targetUserId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }
    public void unsubscribeUserGlobalStatus(long targetUserId, String sessionId) {
        Set<String> sessions = watchers.get(targetUserId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                watchers.remove(targetUserId);
            }
        }
    }

    public boolean updateUserAction(long chatId, long userId, String action) {
        String key = chatId + ":" + userId;
        String previous = userChatActions.put(key, action);
        // Если действие не изменилось (или это первое действие), возвращаем false
        return previous == null || !previous.equals(action);
    }
}