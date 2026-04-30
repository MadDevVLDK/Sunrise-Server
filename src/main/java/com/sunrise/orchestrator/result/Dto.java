package com.sunrise.orchestrator.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sunrise.db.event.ChatEvent;
import com.sunrise.orchestrator.type.ChatType;
import com.sunrise.orchestrator.type.MessageType;
import com.sunrise.orchestrator.type.TokenType;

import java.time.Instant;
import java.util.List;

public final class Dto {

    
    // ==================== ЧАТЫ ====================

    public record ChatMeta(
        long id,
        @JsonProperty("isPinned") boolean isPinned,
        Long lastMsgId,
        int unreadCount,
        long seq
    ) {}

    public record ChatProfile(
        long id,
        String name,
        String description,
        ChatType chatType,
        ChatMemberFull rights,
        ChatMemberProfileFull opponent, // только для личных чатов
        int membersCount,
        Message lastMessage,
        Long lastReadMessageId,
        int unreadCount,
        long seq,
        Instant updatedAt,
        Instant createdAt,
        long createdBy
    ) {}

    public record ChatSecurity(
        long id,
        ChatType chatType,
        int membersCount,
        Instant createdAt,
        long createdBy,
        Instant deletedAt,
        @JsonProperty("isDeleted") boolean isDeleted
    ) { }

    public record ChatMembersPage(
        List<ChatMemberProfileFull> chatMembers,
        Long nextCursor
    ) {}

    public record UserChatsPage(
        List<ChatProfile> chats,
        Long nextCursor
    ) {}

    public record ChatStatsResult(
        int totalMessages, 
        int deletedForAll, 
        boolean canDeleteForAll
    ) { }


    // ==================== УЧАСТНИКИ ЧАТОВ ====================

    public record ChatMemberFull(
        long chatId,
        long userId,
        String tag,
        Instant settingsUpdatedAt,
        Instant updatedAt,
        Instant joinedAt,
        @JsonProperty("isPinned") boolean isPinned,
        @JsonProperty("isAdmin") boolean isAdmin
    ) {}

    public record ChatMemberProfile(
        long chatId,
        long userId,
        String tag,
        Instant updatedAt,
        Instant joinedAt,
        @JsonProperty("isAdmin") boolean isAdmin
    ) {}

    public record ChatMemberProfileFull(
        UserProfileLight userProfile,
        ChatMemberProfile memberProfile
    ) {}


    // ==================== СООБЩЕНИЯ ====================

    public record MessagesPage(
        List<Message> messages,
        Long nextCursor
    ) {}

    public record Message(
        long id,
        long chatId,
        MessageType messageType,
        long senderId,
        Instant profileUpdatedAt,
        Instant memberUpdatedAt,
        String text,
        long readCount,
        Instant sentAt,
        Instant updatedAt,
        Instant deletedAt,
        @JsonProperty("isDeleted") boolean isDeleted
    ) {}

    public record MessageReadStatus(
        long userId,
        Instant readAt
    ) {}


    // ==================== ПОЛЬЗОВАТЕЛИ ====================

    public record UserLogin(
        String jwtToken, 
        java.util.Date expiration
    ) { }

    public record UserProfileLight(
        long id,
        String username,
        String name,
        Instant profileUpdatedAt,
        Instant createdAt
    ) {}

    public record UserProfileFull(
        long id,
        String username,
        String name,
        Instant profileUpdatedAt,
        Instant createdAt
    ) {}

    public record UserSecurity(
        long id,
        String email,
        String hashPassword,
        int jwtVersion,
        @JsonProperty("isEnabled") boolean isEnabled,
        Instant deletedAt,
        @JsonProperty("isDeleted") boolean isDeleted
    ) {}

    public record UsersPage(
        List<UserProfileLight> users,
        Long nextCursor
    ) {}


    // ==================== ТОКЕНЫ ====================

    public record VerificationToken(
        long id,
        long userId,
        String token,
        TokenType tokenType,
        Instant expiryDate,
        Instant createdAt
    ) { }


    // ==================== СОБЫТИЯ ====================

    public record GlobalChatEvent(
        long seq, 
        String type, 
        ChatEvent.IChatEvent event
    ) {}

    public record GlobalChatSync(
        List<GlobalChatEvent> events, 
        boolean hasMore
    ) {}
}