package com.sunrise.web.websocket.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sunrise.db.service.EventDbService.ChatEvent;
import com.sunrise.db.service.EventDbService.UserEvent;
import com.sunrise.web.websocket.event.WsAppEvent.*;
import com.sunrise.web.websocket.service.WebSocketNotifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsAppEventListener {

    private final WebSocketNotifier wsNotify;

    
    // ========================== USER =============================
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserChatCreated(UserChatCreated event) {
        UserEvent userEvent = event.event();
        var data = event.data();
        wsNotify.notifyUserEvent(userEvent, data);
        log.debug("[💬] 🔔 Notified user {} about chat creation (tempId={}, chatId={})", userEvent.userId(), data.tempId(), data.chatId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserChatAdded(UserChatAdded event) {
        UserEvent userEvent = event.event();
        var data = event.data();
        wsNotify.notifyUserEvent(userEvent, data);
        log.debug("[💬] 🔔 Notified user {} about being added to chat {}", userEvent.userId(), data.chatId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserChatRemoved(UserChatRemoved event) {
        UserEvent userEvent = event.event();
        var data = event.data();
        wsNotify.notifyUserEvent(userEvent, data);
        log.debug("[💬] 🔔 Notified user {} about removal from chat {}", userEvent.userId(), data.chatId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserChatDeleted(UserChatDeleted event) {
        UserEvent userEvent = event.event();
        var data = event.data();
        wsNotify.notifyUserEvent(userEvent, data);
        log.debug("[💬] 🔔 Notified user {} about chat deletion {}", userEvent.userId(), data.chatId());
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserChatSettingsChanged(UserChatSettingsChanged event) {
        UserEvent userEvent = event.event();
        var data = event.data();
        wsNotify.notifyUserEvent(userEvent, data);
        log.debug("[💬] 🔔 Notified user {} of settings update: pinned={} (chatId={})", userEvent.userId(), data.isPinned(), data.chatId());
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserChatMessageSent(UserChatMessageSent event) {
        UserEvent userEvent = event.event();
        var data = event.data();
        wsNotify.notifyUserEvent(userEvent, data);
        log.debug("[💬] 🔔 Notified user {} of new message {} in chat {}", userEvent.userId(), data.messageId(), data.chatId());
    }


    // ========================= CHAT =============================
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatUpdated(ChatUpdated event) {
        ChatEvent chatEvent = event.event();
        var data = event.data();
        wsNotify.notifyChatEvent(chatEvent, data);
        log.debug("[💬] 🔔 Notified chat {} info update", data.chatId());
    }

    
    // ====================== CHAT-MEMBER =========================

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMemberAdded(ChatMemberAdded event) {
        ChatEvent chatEvent = event.event();
        var data = event.data();
        wsNotify.notifyChatEvent(chatEvent, data);
        log.debug("[💬] 🔔 Notified new member {} in chat {}", data.userId(), data.chatId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMembersAdded(ChatMembersAdded event) {
        ChatEvent chatEvent = event.event();
        var data = event.data();
        wsNotify.notifyChatEvent(chatEvent, data);
        log.debug("[💬] 🔔 Notified batch of {} new members in chat {}", data.userIds().size(), data.chatId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMemberInfoUpdated(ChatMemberInfoUpdated event) {
        ChatEvent chatEvent = event.event();
        var data = event.data();
        wsNotify.notifyChatEvent(chatEvent, data);
        log.debug("[💬] 🔔 Notified member {} info update in chat {}", data.userId(), data.chatId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMemberAdminUpdated(ChatMemberAdminUpdated event) {
        ChatEvent chatEvent = event.event();
        var data = event.data();
        wsNotify.notifyChatEvent(chatEvent, data);
        log.debug("[💬] 🔔 Notified member {} admin rights update (isAdmin={}) in chat {}", data.userId(), data.isAdmin(), data.chatId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMemberRemoved(ChatMemberRemoved event) {
        ChatEvent chatEvent = event.event();
        var data = event.data();
        wsNotify.notifyChatEvent(chatEvent, data);
        log.debug("[💬] 🔔 Notified member {} removal from chat {}", data.userId(), data.chatId());
    }


    // ======================= MESSAGE ===========================

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageCreatedFull(WsAppEvent.MessageCreatedFull event) {
        ChatEvent chatEvent = event.event();
        var data = event.data();
        wsNotify.notifyChatEvent(chatEvent, data);
        log.debug("[💬] 🔔 Full message {} (eventId={}) sent to chat {}", data.messageId(), chatEvent.eventId(), data.chatId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageInfoUpdated(MessageInfoUpdated event) {
        ChatEvent chatEvent = event.event();
        var data = event.data();
        wsNotify.notifyChatEvent(chatEvent, data);
        log.debug("[💬] 🔔 Notified message {} update in chat {}", data.messageId(), data.chatId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageDeleted(MessageDeleted event) {
        ChatEvent chatEvent = event.event();
        var data = event.data();
        wsNotify.notifyChatEvent(chatEvent, data);
        log.debug("[💬] 🔔 Notified message {} deletion in chat {}", data.messageId(), data.chatId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessagesReadUpTo(MessagesReadUpTo event) {
        ChatEvent chatEvent = event.event();
        var data = event.data();
        wsNotify.notifyChatEvent(chatEvent, data);
        log.debug("[💬] 🔔 Notified user {} read up to message {} in chat {}", data.userId(), data.upToMessageId(), data.chatId());
    }
}
