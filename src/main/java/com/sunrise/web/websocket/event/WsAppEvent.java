package com.sunrise.web.websocket.event;

import com.sunrise.db.service.EventDbService.ChatEvent;
import com.sunrise.db.service.EventDbService.UserEvent;
import com.sunrise.orchestrator.event.IDomainEvent;

public final class WsAppEvent {


    // ====================== USERS-EVENTS ========================

    public record UserChatCreated(UserEvent event, IDomainEvent.UserChatCreated data) {}
    public record UserChatAdded(UserEvent event, IDomainEvent.UserChatAdded data) {}
    public record UserChatRemoved(UserEvent event, IDomainEvent.UserChatRemoved data) {}
    public record UserChatDeleted(UserEvent event, IDomainEvent.UserChatDeleted data) {}
    public record UserChatSettingsChanged(UserEvent event, IDomainEvent.UserChatSettingsChanged data) {}
    public record UserChatMessageSent(UserEvent event, IDomainEvent.UserChatMessageSent data) {}


    // ========================= CHAT =============================
    
    public record ChatUpdated(ChatEvent event, IDomainEvent.ChatUpdated data) {}


    // ====================== CHAT-MEMBER =========================
    
    public record ChatMemberAdded(ChatEvent event, IDomainEvent.ChatMemberAdded data) {}
    public record ChatMembersAdded(ChatEvent event, IDomainEvent.ChatMembersAdded data) {}
    public record ChatMemberInfoUpdated(ChatEvent event, IDomainEvent.ChatMemberInfoUpdate data) {}
    public record ChatMemberAdminUpdated(ChatEvent event, IDomainEvent.ChatMemberAdminUpdated data) {}
    public record ChatMemberRemoved(ChatEvent event, IDomainEvent.ChatMemberRemoved data) {}


    // ======================= MESSAGE ===========================
    
    public record MessageCreatedFull(ChatEvent event, IDomainEvent.MessageCreatedFull data) {}
    public record MessageCreated(ChatEvent event, IDomainEvent.MessageCreated data) {}
    public record MessageInfoUpdated(ChatEvent event, IDomainEvent.MessageUpdated data) {}
    public record MessageDeleted(ChatEvent event, IDomainEvent.MessageDeleted data) {}
    public record MessagesReadUpTo(ChatEvent event, IDomainEvent.MessagesReadUpTo data) {}
}