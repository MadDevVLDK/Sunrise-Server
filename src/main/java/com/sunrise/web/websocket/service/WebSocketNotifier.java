package com.sunrise.web.websocket.service;

import com.sunrise.db.service.EventDbService.ChatEvent;
import com.sunrise.db.service.EventDbService.UserEvent;
import com.sunrise.orchestrator.event.EventType;
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


    // ====================== USERS-EVENTS ========================

    @Async("webSocketNotifierExecutor")
    public void notifyUserChatCreated(UserEvent event, IDomainEvent.UserChatCreated data) {
        sendToUserEventsQueue(event.userId(), new GlobalEvent(
            event.eventId(), EventType.USER_CHAT_CREATED, data, data.getCreatedAt()
        ));
        log.debug("[💬] 🔔 Notified user {} about chat creation (tempId={}, chatId={})", event.userId(), data.tempId(), data.chatId());
    }

    @Async("webSocketNotifierExecutor")
    public void notifyUserChatAdded(UserEvent event, IDomainEvent.UserChatAdded data) {
        sendToUserEventsQueue(event.userId(), new GlobalEvent(
            event.eventId(), EventType.USER_CHAT_ADDED, data, data.getCreatedAt()
        ));
        log.debug("[💬] 🔔 Notified user {} about being added to chat {}", event.userId(), data.chatId());
    }

    @Async("webSocketNotifierExecutor")
    public void notifyUserChatRemoved(UserEvent event, IDomainEvent.UserChatRemoved data) {
        sendToUserEventsQueue(event.userId(), new GlobalEvent(
            event.eventId(), EventType.USER_CHAT_REMOVED, data, data.getCreatedAt()
        ));
        log.debug("[💬] 🔔 Notified user {} about removal from chat {}", event.userId(), data.chatId());
    }

    @Async("webSocketNotifierExecutor")
    public void notifyUserChatDeleted(UserEvent event, IDomainEvent.UserChatDeleted data) {
        sendToUserEventsQueue(event.userId(), new GlobalEvent(
            event.eventId(), EventType.USER_CHAT_DELETED, data, data.getCreatedAt()
        ));
        log.debug("[💬] 🔔 Notified user {} about chat deletion {}", event.userId(), data.chatId());
    }

    @Async("webSocketNotifierExecutor")
    public void notifyUserChatSettingsChanged(UserEvent event, IDomainEvent.UserChatSettingsChanged data) {
        sendToUserEventsQueue(event.userId(), new GlobalEvent(
            event.eventId(), EventType.USER_CHAT_SETTINGS_CHANGED, data, data.getCreatedAt()
        ));
        log.debug("[💬] 🔔 Notified user {} of settings update: pinned={} (chatId={})", event.userId(), data.isPinned(), data.chatId());
    }

    @Async("webSocketNotifierExecutor")
    public void notifyUserChatMessageSent(UserEvent event, IDomainEvent.UserChatMessageSent data) {
        sendToUserEventsQueue(event.userId(), new GlobalEvent(
            event.eventId(), EventType.USER_CHAT_MESSAGE_SENT, data, data.getCreatedAt()
        ));
        log.debug("[💬] 🔔 Notified user {} of new message {} in chat {}", event.userId(), data.messageId(), data.chatId());
    }


    // ========================= CHAT =============================

    @Async("webSocketNotifierExecutor")
    public void notifyChatUpdated(ChatEvent event, IDomainEvent.ChatUpdated data) {
        sendToChatTopic(data.chatId(), new GlobalEvent(
            event.eventId(), EventType.CHAT_UPDATED, data, data.getCreatedAt()
        ));
        log.debug("[💬] 🔔 Notified chat {} info update with event {}", data.chatId(), event);
    }


    // ====================== CHAT-MEMBER =========================

    @Async("webSocketNotifierExecutor")
    public void notifyChatMemberAdded(ChatEvent event, IDomainEvent.ChatMemberAdded data) {
        sendToChatTopic(data.chatId(), new GlobalEvent(
            event.eventId(), EventType.CHAT_MEMBER_ADDED, data, data.getCreatedAt()
        ));
        log.debug("[💬] 🔔 Notified new member {} in chat {} with event {}", data.userId(), data.chatId(), event);
    }

    @Async("webSocketNotifierExecutor")
    public void notifyChatMembersAdded(ChatEvent event, IDomainEvent.ChatMembersAdded data) {
        sendToChatTopic(data.chatId(), new GlobalEvent(
            event.eventId(), EventType.CHAT_MEMBERS_ADDED, data, data.getCreatedAt()
        ));
        log.debug("[💬] 🔔 Notified batch of {} new members in chat {} with event {}", data.userIds().size(), data.chatId(), event);
    }

    @Async("webSocketNotifierExecutor")
    public void notifyChatMemberInfoUpdated(ChatEvent event, IDomainEvent.ChatMemberInfoUpdate data) {
        sendToChatTopic(data.chatId(), new GlobalEvent(
            event.eventId(), EventType.CHAT_MEMBER_INFO_UPDATE, data, data.getCreatedAt()
        ));
        log.debug("[💬] 🔔 Notified member {} info update in chat {} with event {}", data.userId(), data.chatId(), event);
    }

    @Async("webSocketNotifierExecutor")
    public void notifyChatMemberAdminUpdated(ChatEvent event, IDomainEvent.ChatMemberAdminUpdated data) {
        sendToChatTopic(data.chatId(), new GlobalEvent(
            event.eventId(), EventType.CHAT_MEMBER_ADMIN_UPDATED, data, data.getCreatedAt()
        ));
        log.debug("[💬] 🔔 Notified member {} admin rights update (isAdmin={}) in chat {} with event {}", data.userId(), data.isAdmin(), data.chatId(), event);
    }

    @Async("webSocketNotifierExecutor")
    public void notifyChatMemberRemoved(ChatEvent event, IDomainEvent.ChatMemberRemoved data) {
        sendToChatTopic(data.chatId(), new GlobalEvent(
            event.eventId(), EventType.CHAT_MEMBER_REMOVED, data, data.getCreatedAt()
        ));
        log.debug("[💬] 🔔 Notified member {} removal from chat {} with event {}", data.userId(), data.chatId(), event);
    }


    // ======================= MESSAGE ===========================

    @Async("webSocketNotifierExecutor")
    public void notifyMessageCreatedFull(ChatEvent event, IDomainEvent.MessageCreatedFull data) {
        sendToChatTopic(data.chatId(), new GlobalEvent(
            event.eventId(), EventType.MESSAGE_CREATED_FULL, data, data.createdAt()
        ));
        log.debug("[💬] 🔔 Full message {} (eventId={}) sent to chat {}", data.messageId(), event.eventId(), data.chatId());
    }

    @Async("webSocketNotifierExecutor")
    public void notifyMessageInfoUpdated(ChatEvent event, IDomainEvent.MessageUpdated data) {
        sendToChatTopic(data.chatId(), new GlobalEvent(
            event.eventId(), EventType.MESSAGE_UPDATED, data, data.getCreatedAt()
        ));
        log.debug("[💬] 🔔 Notified message {} update in chat {}", data.messageId(), data.chatId());
    }

    @Async("webSocketNotifierExecutor")
    public void notifyMessageDeleted(ChatEvent event, IDomainEvent.MessageDeleted data) {
        sendToChatTopic(data.chatId(), new GlobalEvent(
            event.eventId(), EventType.MESSAGE_DELETED, data, data.getCreatedAt()
        ));
        log.debug("[💬] 🔔 Notified message {} deletion in chat {} with event {}", data.messageId(), data.chatId(), event);
    }

    @Async("webSocketNotifierExecutor")
    public void notifyMessagesReadUpTo(ChatEvent event, IDomainEvent.MessagesReadUpTo data) {
        sendToChatTopic(data.chatId(), new GlobalEvent(
            event.eventId(), EventType.MESSAGES_READ_UP_TO, data, data.getCreatedAt()
        ));
        log.debug("[💬] 🔔 Notified user {} read up to message {} in chat {} with event {}", data.userId(), data.upToMessageId(), data.chatId(), event);
    }


    // ================= PRESENCE/STATUS/OTHER ====================

    @Async("webSocketNotifierExecutor")
    public void notifyUserStatusChangeToSubscriber(long userId, String sessionId, long actorUserId, String newStatus) {
        sendToUserSessionQueue(userId, sessionId, "/user-statuses", new WsResponse.UserStatus(actorUserId, newStatus));
        log.debug("[💬] 🔔 Sent initial user status: target={} status={} to subscriber {} session={}", actorUserId, newStatus, userId, sessionId);
    }

    @Async("webSocketNotifierExecutor")
    public void notifyUserStatusChange(long actorUserId, String newStatus) {
        sendToUserGlobalStatusTopic(actorUserId, new WsResponse.UserStatus(actorUserId, newStatus));
        log.debug("[💬] 🟢 Notified user {} status change to '{}'", actorUserId, newStatus);
    }


    @Async("webSocketNotifierExecutor")
    public void notifyUserActionToSubscriber(long userId, String sessionId, long chatId, long actorUserId, String action) {
        sendToUserSessionQueue(userId, sessionId, "/chat-actions", new WsResponse.UserChatAction(actorUserId, chatId, action));
        log.debug("[💬] 🔔 Sent initial chat action: user={} action='{}' in chat={} to subscriber {} session={}", actorUserId, action, chatId, userId, sessionId);
    }

    @Async("webSocketNotifierExecutor")
    public void notifyUserAction(long chatId, long userId, String action) {
        sendToUserChatActionsTopic(chatId, new WsResponse.UserChatAction(userId, chatId, action));
        log.debug("[💬] 🔔 Notified user {} action '{}' in chat {}", userId, action, chatId);
    }


    @Async("webSocketNotifierExecutor")
    public void notifyPong(long userId, @NonNull String sessionId) {
        sendToUserSessionQueue(userId, sessionId, "/pong", new WsResponse.Pong());
        log.debug("[💬] 🏓 Sent pong to session {}", sessionId);
    }

    @Async("webSocketNotifierExecutor")
    public void notifyError(long userId, @NonNull String sessionId, String error, String errorUrl) {
        sendToUserSessionQueue(userId, sessionId, "/errors", new WsResponse.Error(error, errorUrl));
        log.debug("[💬] ⚠️ Sent error notification to session {}: {}", sessionId, error);
    }


    // ======================= PRIVATE ============================

    private void sendToUserSessionQueue(long userId, @NonNull String sessionId, String path, @NonNull Object result) {
        try {
            messagingTemplate.convertAndSendToUser(
                userId + "", "/queue" + path, result, Map.of("simpSessionId", sessionId)
            );
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to send to user session queue: userId={}, sessionId={}, path={}, error={}", userId, sessionId, path, e.getMessage(), e);
        }
    }
    
    private void sendToUserEventsQueue(long userId, @NonNull GlobalEvent result) {
        try {
            messagingTemplate.convertAndSendToUser(userId + "", "/queue/user-events", result);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to send user events to userId={}: {}", userId, e.getMessage(), e);
        }
    }
    
    private void sendToChatTopic(long chatId, @NonNull GlobalEvent result) {
        try {
            messagingTemplate.convertAndSend("/topic/chats/" + chatId, result);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to send to chat topic chatId={}: {}", chatId, e.getMessage(), e);
        }
    }

    private void sendToUserChatActionsTopic(long chatId, @NonNull Object result) {
        try {
            messagingTemplate.convertAndSend("/topic/chats/" + chatId + "/actions", result);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to send to chat actions topic chatId={}: {}", chatId, e.getMessage(), e);
        }
    }

    private void sendToUserGlobalStatusTopic(long actorUserId, @NonNull Object result) {
        try {
            messagingTemplate.convertAndSend("/topic/users/" + actorUserId + "/statuses", result);
        } catch (Exception e) {
            log.error("[💬] ❌ Failed to send to user global status topic userId={}: {}", actorUserId, e.getMessage(), e);
        }
    }
}