package com.sunrise.notifier;

import com.sunrise.orchestrator.result.ChatEvent;
import com.sunrise.orchestrator.result.Dto;
import com.sunrise.orchestrator.result.UserEvent;
import com.sunrise.orchestrator.result.UserEvent.ChatCreatedWithMembers.MemberInfo;
import com.sunrise.web.payload.WsResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
    public void notifyMessageNew(ChatEvent.PublicMessageCreated event, long seq) {
        try {
            var globalEvent = new Dto.GlobalChatEvent(seq, "PUBLIC_MESSAGE_CREATED", event);
            sendToChatTopic(event.chatId(), globalEvent);
            log.debug("[💬] 📨 Notified new message {} in chat {} with seq {}", event.messageId(), event.chatId(), seq);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify new message {} in chat {}: {}", event.messageId(), event.chatId(), e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyMessagePrivateNew(ChatEvent.PrivateMessageCreated event, long seq) {
        try {
             var globalEvent = new Dto.GlobalChatEvent(seq, "PRIVATE_MESSAGE_CREATED", event);
            for (long userId : List.of(event.senderId(), event.receiverId())) {
                sendToUserSessions(userId, "/private-messages", globalEvent);
            }
            log.debug("[💬] 🔒 Notified private message {} from user {} to user {}", event.messageId(), event.senderId(), event.receiverId());
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify private message {}: {}", event.messageId(), e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyMessageInfoUpdated(ChatEvent.MessageUpdated event, long seq) {
        try {
            var globalEvent = new Dto.GlobalChatEvent(seq, "MESSAGE_UPDATED", event);
            sendToChatTopic(event.chatId(), globalEvent);
            log.debug("[💬] ✏️ Notified message {} update in chat {}", event.messageId(), event.chatId());
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify message {} update: {}", event.messageId(), e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyMessageDeleted(ChatEvent.MessageDeleted event, long seq) {
        try {
            var globalEvent = new Dto.GlobalChatEvent(seq, "MESSAGE_DELETED", event);
            sendToChatTopic(event.chatId(), globalEvent);
            log.debug("[💬] 🗑️ Notified message {} deletion in chat {} with seq {}", event.messageId(), event.chatId(), seq);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify message {} deletion: {}", event.messageId(), e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyMessagesReadUpTo(ChatEvent.MessagesReadUpTo event, long seq) {
        try {
            var globalEvent = new Dto.GlobalChatEvent(seq, "MESSAGES_READ_UP_TO", event);
            sendToChatTopic(event.chatId(), globalEvent);
            log.debug("[💬] 👁️ Notified user {} read up to message {} in chat {} with seq {}", event.userId(), event.upToMessageId(), event.chatId(), seq);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify read status for user {} in chat {}: {}", event.userId(), event.chatId(), e.getMessage());
        }
    }


    // ========================= CHAT =============================
    
    @Async("webSocketNotifierExecutor")
    public void notifyChatWithMembersNew(long tempId, UserEvent.ChatCreatedWithMembers event, long seq) {
        try {
            var globalEvent = new Dto.GlobalUserEvent(seq, "CHAT_MEMBER_SETTINGS_UPDATED", event);
            for (MemberInfo member : event.members()){
                sendToUserSessions(member.userId(), "/chats", globalEvent);
            }
            log.debug("[💬] 💑 Notified new chat {} creation to {} users", event.chatId(), event.members().size());
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify new chat {} creation: {}", event.chatId(), e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyChatUpdated(ChatEvent.ChatUpdated event, long seq) {
        try {
            var globalEvent = new Dto.GlobalChatEvent(seq, "CHAT_UPDATED", event);
            sendToChatTopic(event.chatId(), globalEvent);
            log.debug("[💬] 📝 Notified chat {} info update with seq {}", event.chatId(), seq);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify chat {} info update: {}", event.chatId(), e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifySelfChatSettingsUpdated(UserEvent.ChatMemberSettingsUpdated event, long seq) {
        try {
            var globalEvent = new Dto.GlobalUserEvent(seq, "CHAT_MEMBER_SETTINGS_UPDATED", event);
            sendToUserSessions(event.userId(), "/chat-settings", globalEvent);
            log.debug("[💬] ⚙️ Notified user {} chat {} settings update (pinned={})", event.userId(), event.chatId(), event.isPinned());
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify user {} chat {} settings update: {}", event.userId(), event.chatId(), e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyChatDeleted(ChatEvent.ChatDeleted event, long seq) {
        try {
            var globalEvent = new Dto.GlobalChatEvent(seq, "CHAT_DELETED", event);
            sendToChatTopic(event.chatId(), globalEvent);
            log.debug("[💬] 🗑️ Notified chat {} deletion with seq {}", event.chatId(), seq);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify chat {} deletion: {}", event.chatId(), e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyChatRestored(ChatEvent.ChatRestored event, long seq) {
        try {
            var globalEvent = new Dto.GlobalChatEvent(seq, "CHAT_RESTORED", event);
            sendToChatTopic(event.chatId(), globalEvent);
            log.debug("[💬] 🔄 Notified chat {} restoration with seq {}", event.chatId(), seq);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify chat {} restoration: {}", event.chatId(), e.getMessage());
        }
    }


    // ===================== CHAT-MEMBER ===========================
    
    @Async("webSocketNotifierExecutor")
    public void notifyChatMemberNew(ChatEvent.ChatMemberAdded event, long seq) {
        try {
            var globalEvent = new Dto.GlobalChatEvent(seq, "CHAT_MEMBER_ADDED", event);
            sendToChatTopic(event.chatId(), globalEvent);
            log.debug("[💬] 👤 Notified new member {} in chat {} with seq {}", event.userId(), event.chatId(), seq);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify new member {} in chat {}: {}", event.userId(), event.chatId(), e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyChatMembersNew(ChatEvent.ChatMembersAdded event, long seq) {
        try {
            var globalEvent = new Dto.GlobalChatEvent(seq, "CHAT_MEMBERS_ADDED", event);
            sendToChatTopic(event.chatId(), globalEvent);
            log.debug("[💬] 👥 Notified batch of {} new members in chat {} with seq {}", event.userIds().size(), event.chatId(), seq);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify batch of new members in chat {}: {}", event.chatId(), e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyChatMemberInfoUpdated(ChatEvent.ChatMemberInfoUpdate event, long seq) {
        try {
            var globalEvent = new Dto.GlobalChatEvent(seq, "CHAT_MEMBER_INFO_UPDATE", event);
            sendToChatTopic(event.chatId(), globalEvent);
            log.debug("[💬] 📋 Notified member {} info update in chat {} with seq {}", event.userId(), event.chatId(), seq);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify member {} info update in chat {}: {}", event.userId(), event.chatId(), e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyChatMemberAdminRightsUpdated(ChatEvent.ChatMemberAdminUpdated event, long seq) {
        try {
            var globalEvent = new Dto.GlobalChatEvent(seq, "CHAT_MEMBER_ADMIN_UPDATED", event);
            sendToChatTopic(event.chatId(), globalEvent);
            log.debug("[💬] 👑 Notified member {} admin rights update (isAdmin={}) in chat {} with seq {}", event.userId(), event.isAdmin(), event.chatId(), seq);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify member {} admin rights update in chat {}: {}", event.userId(), event.chatId(), e.getMessage());
        }
    }

    @Async("webSocketNotifierExecutor")
    public void notifyChatMemberRemoved(ChatEvent.ChatMemberRemoved event, long seq) {
        try {
            Dto.GlobalChatEvent globalEvent = new Dto.GlobalChatEvent(seq, "CHAT_MEMBER_REMOVED", event);
            sendToChatTopic(event.chatId(), globalEvent);
            log.debug("[💬] 🚪 Notified member {} removal from chat {} with seq {}", event.userId(), event.chatId(), seq);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to notify member {} removal from chat {}: {}", event.userId(), event.chatId(), e.getMessage());
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