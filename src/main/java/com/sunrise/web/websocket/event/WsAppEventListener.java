package com.sunrise.web.websocket.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sunrise.web.websocket.event.WsAppEvent.*;
import com.sunrise.web.websocket.service.WebSocketNotifier;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WsAppEventListener {

    private final WebSocketNotifier wsNotify;

    
    // ====================== USERS-EVENTS ========================
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserChatCreated(UserChatCreated event) {
        wsNotify.notifyUserChatCreated(event.event(), event.data());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserChatAdded(UserChatAdded event) {
        wsNotify.notifyUserChatAdded(event.event(), event.data());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserChatRemoved(UserChatRemoved event) {
        wsNotify.notifyUserChatRemoved(event.event(), event.data());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserChatDeleted(UserChatDeleted event) {
        wsNotify.notifyUserChatDeleted(event.event(), event.data());
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserChatSettingsChanged(UserChatSettingsChanged event) {
        wsNotify.notifyUserChatSettingsChanged(event.event(), event.data());
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserChatMessageSent(UserChatMessageSent event) {
        wsNotify.notifyUserChatMessageSent(event.event(), event.data());
    }


    // ========================= CHAT =============================
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatUpdated(ChatUpdated event) {
        wsNotify.notifyChatUpdated(event.event(), event.data());
    }

    
    // ====================== CHAT-MEMBER =========================

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMemberAdded(ChatMemberAdded event) {
        wsNotify.notifyChatMemberAdded(event.event(), event.data());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMembersAdded(ChatMembersAdded event) {
        wsNotify.notifyChatMembersAdded(event.event(), event.data());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMemberInfoUpdated(ChatMemberInfoUpdated data) {
        wsNotify.notifyChatMemberInfoUpdated(data.event(), data.data());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMemberAdminUpdated(ChatMemberAdminUpdated event) {
        wsNotify.notifyChatMemberAdminUpdated(event.event(), event.data());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMemberRemoved(ChatMemberRemoved event) {
        wsNotify.notifyChatMemberRemoved(event.event(), event.data());
    }


    // ======================= MESSAGE ===========================

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageCreatedFull(WsAppEvent.MessageCreatedFull event) {
        wsNotify.notifyMessageCreatedFull(event.event(), event.data());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageInfoUpdated(MessageInfoUpdated event) {
        wsNotify.notifyMessageInfoUpdated(event.event(), event.data());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageDeleted(MessageDeleted event) {
        wsNotify.notifyMessageDeleted(event.event(), event.data());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessagesReadUpTo(MessagesReadUpTo event) {
        wsNotify.notifyMessagesReadUpTo(event.event(), event.data());
    }
}
