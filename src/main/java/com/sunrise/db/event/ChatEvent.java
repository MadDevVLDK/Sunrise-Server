package com.sunrise.db.event;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ChatEvent {

    public interface IChatEvent {
        @JsonIgnore long getChatId();
        @JsonIgnore Instant getCreatedAtDb();
    }
    
    // ==================== СОБЫТИЯ СООБЩЕНИЙ ====================

    public record MessageCreated(
        long chatId,
        long messageId,
        long senderId,
        String text,
        Instant sentAt,
        Instant createdAt
    ) implements IChatEvent {
        @Override public long getChatId() { return chatId; }
        @Override public Instant getCreatedAtDb() { return createdAt; }
    }

    public record MessageUpdated(
        long chatId,
        long messageId,
        String newText,
        Instant updatedAt
    ) implements IChatEvent {
        @Override public long getChatId() { return chatId; }
        @Override public Instant getCreatedAtDb() { return updatedAt; }
    }

    public record MessageDeleted(
        long chatId,
        long messageId,
        Instant deletedAt
    ) implements IChatEvent {
        @Override public long getChatId() { return chatId; }
        @Override public Instant getCreatedAtDb() { return deletedAt; }
    }

    public record MessagesReadUpTo(
        long chatId,
        long userId,
        long upToMessageId,
        Instant readAt
    ) implements IChatEvent {
        @Override public long getChatId() { return chatId; }
        @Override public Instant getCreatedAtDb() { return readAt; }
    }


    // ==================== СОБЫТИЯ УЧАСТНИКОВ ====================

    public record ChatMemberAdded(
        long chatId,
        long userId,
        boolean isAdmin,
        Instant joinedAt,
        Instant createdAt
    ) implements IChatEvent {
        @Override public long getChatId() { return chatId; }
        @Override public Instant getCreatedAtDb() { return createdAt; }
    }

    public record ChatMembersAdded(
        long chatId,
        List<Long> userIds,
        List<Boolean> isAdmin,
        Instant joinedAt,
        Instant createdAt
    ) implements IChatEvent {
        @Override public long getChatId() { return chatId; }
        @Override public Instant getCreatedAtDb() { return createdAt; }
    }

    public record ChatMemberInfoUpdate(
        long chatId,
        long userId,
        String tag,
        Instant updatedAt
    ) implements IChatEvent {
        @Override public long getChatId() { return chatId; }
        @Override public Instant getCreatedAtDb() { return updatedAt; }
    }

    public record ChatMemberRemoved(
        long chatId,
        long userId,
        Instant removedAt
    ) implements IChatEvent {
        @Override public long getChatId() { return chatId; }
        @Override public Instant getCreatedAtDb() { return removedAt; }
    }

    public record ChatMemberAdminUpdated(
        long chatId,
        long userId,
        boolean isAdmin,
        Instant updatedAt
    ) implements IChatEvent {
        @Override public long getChatId() { return chatId; }
        @Override public Instant getCreatedAtDb() { return updatedAt; }
    }


    // ==================== СОБЫТИЯ ЧАТА ====================

    public record ChatCreatedWithMembers(
        long chatId,
        String name,
        String description,
        String chatType,
        Long opponentId,
        int membersCount,
        long createdBy,
        List<MemberInfo> members,
        Instant createdAt
    ) implements IChatEvent {
        @Override public long getChatId() { return chatId; }
        @Override public Instant getCreatedAtDb() { return createdAt; }

        public record MemberInfo(long userId, boolean isAdmin, Instant joinedAt) {}
    }

    public record ChatUpdated(
        long chatId,
        String newName,
        String newDescription,
        Instant updatedAt
    ) implements IChatEvent {
        @Override public long getChatId() { return chatId; }
        @Override public Instant getCreatedAtDb() { return updatedAt; }
    }

    public record ChatDeleted(
        long chatId, 
        Instant deletedAt
    ) implements IChatEvent {
        @Override public long getChatId() { return chatId; }
        @Override public Instant getCreatedAtDb() { return deletedAt; }
    }

    public record ChatRestored(
        long chatId, 
        Instant restoredAt
    ) implements IChatEvent {
        @Override public long getChatId() { return chatId; }
        @Override public Instant getCreatedAtDb() { return restoredAt; }
    }
}