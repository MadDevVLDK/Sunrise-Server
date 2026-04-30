package com.sunrise.notifier;

import com.sunrise.core.creation.CreateDto.*;
import com.sunrise.web.payload.WsResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;


@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotifier { // TODO: Добавить batch‑отправку ChatMembersNew

    private final SimpMessagingTemplate messagingTemplate;
    private final SessionRegistry sessionRegistry;


    // ======================= MESSAGE ============================
    
    @Async("webSocketNotifierExecutor")
    public void notifyMessageNew(long tempId, Message message, Instant senderProfileUpdatedAt) {
        try {
            sendToChatTopic(message.getChatId(), new WsResponse.MessageNew(
                tempId, message.getId(), message.getChatId(), message.getSenderId(),
                senderProfileUpdatedAt, message.getText(), message.getReadCount(),
                message.getSentAt(), message.getUpdatedAt(), message.getDeletedAt(), message.isDeleted()
            ));
            log.debug("[💬] 📨 Notified new message {} in chat {}", message.getId(), message.getChatId());
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify new message {} in chat {}: {}", message.getId(), message.getChatId(), e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyMessagePrivateNew(long tempId, Message message, Instant senderProfileUpdatedAt, long receiverId) {
        try {
            var response = new WsResponse.MessagePrivateNew(
                tempId, message.getId(), message.getChatId(), message.getSenderId(),
                senderProfileUpdatedAt, message.getText(), message.getSentAt()
            );
            for (long userId : List.of(message.getSenderId(), receiverId)) {
                sendToUserSessions(userId, "/private-messages", response);
            }
            log.debug("[💬] 🔒 Notified private message {} from user {} to user {}", message.getId(), message.getSenderId(), receiverId);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify private message {}: {}", message.getId(), e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyMessageInfoUpdated(long chatId, long messageId, String newText, Instant updatedAt) {
        try {
            sendToChatTopic(chatId, new WsResponse.MessageUpdate(
                messageId, chatId, newText, updatedAt
            ));
            log.debug("[💬] ✏️ Notified message {} update in chat {}", messageId, chatId);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify message {} update: {}", messageId, e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyMessageDeleted(long chatId, long messageId, Instant deletedAt) {
        try {
            sendToChatTopic(chatId, new WsResponse.MessageDelete(messageId, chatId, deletedAt));
            log.debug("[💬] 🗑️ Notified message {} deletion in chat {}", messageId, chatId);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify message {} deletion: {}", messageId, e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyMessageReadUpTo(long chatId, long userId, long upToMessageId, Instant readAt) {
        try {
            sendToChatTopic(chatId, new WsResponse.MessagesReadUpTo(userId, chatId, upToMessageId, readAt));
            log.debug("[💬] 👁️ Notified user {} read up to message {} in chat {}", userId, upToMessageId, chatId);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify read status for user {} in chat {}: {}", userId, chatId, e.getMessage());
        }
    }


    // ========================= CHAT =============================
    
    @Async("webSocketNotifierExecutor")
    public void notifyGroupChatNew(long tempId, GroupChat chat, Set<Long> userIdsToNotify) {
        try {
            var response = new WsResponse.ChatNew(
                tempId, chat.getId(), chat.getName(), chat.getDescription(),
                chat.getChatType(), chat.getOpponentId(), chat.getMembersCount(),
                chat.getUpdatedAt(), chat.getCreatedAt(), chat.getCreatedBy()
            );
            for (long userId : userIdsToNotify){
                sendToUserSessions(userId, "/chats", response);
            }
            log.debug("[💬] 🆕 Notified group chat {} creation to {} users", chat.getId(), userIdsToNotify.size());
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify group chat {} creation: {}", chat.getId(), e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyPersonalChatNew(long tempId, PersonalChat chat, Set<Long> userIdsToNotify) {
        try {
            var response = new WsResponse.ChatNew(
                tempId, chat.getId(), chat.getName(), chat.getDescription(),
                chat.getChatType(), chat.getOpponentId(), chat.getMembersCount(),
                chat.getUpdatedAt(), chat.getCreatedAt(), chat.getCreatedBy()
            );
            for (long userId : userIdsToNotify){
                sendToUserSessions(userId, "/chats", response);
            }
            log.debug("[💬] 💑 Notified personal chat {} creation to {} users", chat.getId(), userIdsToNotify.size());
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify personal chat {} creation: {}", chat.getId(), e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyChatInfoUpdated(long chatId, String newName, String newDescription, Instant updatedAt) {
        try {
            sendToChatTopic(chatId, new WsResponse.ChatInfoUpdate(
                chatId, newName, newDescription, updatedAt
            ));
            log.debug("[💬] 📝 Notified chat {} info update", chatId);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify chat {} info update: {}", chatId, e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifySelfChatSettingsUpdated(long chatId, long userId, boolean isPinned, Instant updatedAt) {
        try {
            sendToUserSessions(userId, "/chat-settings", new WsResponse.SelfChatSettingsUpdate(chatId, isPinned, updatedAt));
            log.debug("[💬] ⚙️ Notified user {} chat {} settings update (pinned={})", userId, chatId, isPinned);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify user {} chat {} settings update: {}", userId, chatId, e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyChatDeleted(long chatId, Instant deletedAt) {
        try {
            sendToChatTopic(chatId, new WsResponse.ChatDelete(chatId, deletedAt));
            log.debug("[💬] 🗑️ Notified chat {} deletion", chatId);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify chat {} deletion: {}", chatId, e.getMessage());
        }
    }


    // ===================== CHAT-MEMBER ===========================
    
    @Async("webSocketNotifierExecutor")
    public void notifyChatMemberNew(ChatMember chatMember) {
        try {
            sendToChatTopic(chatMember.getChatId(), new WsResponse.ChatMemberNew(
                chatMember.getChatId(), chatMember.getUserId(),
                chatMember.getUpdatedAt(), chatMember.getJoinedAt(), chatMember.isAdmin()
            ));
            log.debug("[💬] 👤 Notified new member {} in chat {}", chatMember.getUserId(), chatMember.getChatId());
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify new member {} in chat {}: {}", chatMember.getUserId(), chatMember.getChatId(), e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyChatMembersNew(Collection<ChatMember> chatMembers) {
        for (ChatMember chatMember : chatMembers) {
            try {
                sendToChatTopic(chatMember.getChatId(), new WsResponse.ChatMemberNew(
                    chatMember.getChatId(), chatMember.getUserId(),
                    chatMember.getUpdatedAt(), chatMember.getJoinedAt(), chatMember.isAdmin()
                ));
            } catch (Exception e) {
                log.error("[💬] ❌ Failed to notify new member {} in chat {}: {}", chatMember.getUserId(), chatMember.getChatId(), e.getMessage());
            }
        }
        log.debug("[💬] 👥 Notified batch of {} new members", chatMembers.size());
    }

    @Async("webSocketNotifierExecutor")
    public void notifyChatMemberInfoUpdated(long chatId, long userId, String tag, Instant updatedAt) {
        try {
            sendToChatTopic(chatId, new WsResponse.ChatMemberInfoUpdate(chatId, userId, tag, updatedAt));
            log.debug("[💬] 📋 Notified member {} info update in chat {}", userId, chatId);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify member {} info update in chat {}: {}", userId, chatId, e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyChatMemberAdminRightsUpdated(long chatId, long userId, boolean isAdmin, Instant updatedAt) {
        try {
            sendToChatTopic(chatId, new WsResponse.ChatMemberAdminRightsUpdate(chatId, userId, isAdmin, updatedAt));
            log.debug("[💬] 👑 Notified member {} admin rights update (isAdmin={}) in chat {}", userId, isAdmin, chatId);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify member {} admin rights update in chat {}: {}", userId, chatId, e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyChatMemberDeleted(long chatId, long userId, Instant deletedAt) {
        try {
            sendToChatTopic(chatId, new WsResponse.ChatMemberDelete(chatId, userId, deletedAt));
            log.debug("[💬] 🚪 Notified member {} removal from chat {}", userId, chatId);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify member {} removal from chat {}: {}", userId, chatId, e.getMessage());
        }
    }


    // ================= PRESENCE/STATUS/OTHER ====================
    
    @Async("webSocketNotifierExecutor")
    public void notifyUserStatusChange(long userId, String newStatus, @NonNull Set<String> userSessionsToNotify) {
        try {
            var response = new WsResponse.UserStatus(userId, newStatus);
            for (String sessionId : userSessionsToNotify) {
                if (sessionId == null) continue;
                messagingTemplate.convertAndSendToUser(sessionId, "/queue/user-status", response);
            }
            log.debug("[💬] 🟢 Notified user {} status change to '{}' ({} sessions)", userId, newStatus, userSessionsToNotify.size());
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify user {} status change: {}", userId, e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyUserAction(long chatId, long userId, String action) {
        try {
            sendToChatTopic(chatId, new WsResponse.UserChatAction(userId, chatId, action));
            log.debug("[💬] ⌨️ Notified user {} action '{}' in chat {}", userId, action, chatId);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify user {} action in chat {}: {}", userId, chatId, e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyPong(@NonNull String sessionId) {
        try {
            sendToUserSession(sessionId, "/pong", new WsResponse.Pong());
            log.debug("[💬] 🏓 Sent pong to session {}", sessionId);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to send pong to session {}: {}", sessionId, e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyError(@NonNull String sessionId, String error, String errorUrl) {
        try {
            sendToUserSession(sessionId, "/errors", new WsResponse.Error("websocket_error", error, errorUrl));
            log.debug("[💬] ⚠️ Sent error notification to session {}: {}", sessionId, error);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to send error to session {}: {}", sessionId, e.getMessage());
        }
    }


    // ===== PRIVATE =====
    private void sendToUserSession(@NonNull String sessionId, String path, @NonNull Object result) {
        messagingTemplate.convertAndSendToUser(sessionId, "/queue" + path, result);
    }
    private void sendToUserSessions(long userId, String path, @NonNull Object result) {
        Set<String> sessions = sessionRegistry.getUserSessions(userId);
        if (sessions.isEmpty()) return;

        for (String sessionId : sessions) {
            sendToUserSession(sessionId, "/queue" + path, result);
        }
    }
    private void sendToChatTopic(long chatId, @NonNull Object result) {
        messagingTemplate.convertAndSend("/topic/chats/" + chatId, result);
    }
}