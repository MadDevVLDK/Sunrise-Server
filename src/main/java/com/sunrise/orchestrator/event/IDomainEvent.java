package com.sunrise.orchestrator.event;

import java.time.Instant;
import java.util.List;

public interface IDomainEvent {

    public abstract Instant getCreatedAt();


    // ====================== USERS-EVENTS ========================

    public record UserChatCreated(
        long chatId,
        String tempId,
        Instant createdAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return createdAt; }
    }

    public record UserChatAdded(
        long chatId,
        Instant createdAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return createdAt; }
    }

    public record UserChatRemoved(
        long chatId,
        Instant createdAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return createdAt; }
    }

    public record UserChatDeleted(
        long chatId,
        Instant createdAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return createdAt; }
    }

    public record UserChatSettingsChanged(
        long chatId,
        boolean isPinned,
        Instant updatedAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return updatedAt; }
    }

    public record UserChatMessageSent(
        long chatId,
        long messageId,
        Instant sentAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return sentAt; }
    }


    // ==================== СОБЫТИЯ ЧАТА ====================

    public record ChatCreated(
        String tempId,
        long chatId, 
        Instant createdAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return createdAt; }
    }

    public record ChatUpdated(
        long chatId,
        String newName,
        String newDescription,
        Instant updatedAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return updatedAt; }
    }

    public record ChatDeleted(
        long chatId, 
        Instant deletedAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return deletedAt; }
    }

    
    // ==================== CHAT-MEMBER ====================

    public record ChatMemberAdded(
        long chatId,
        long userId,
        boolean isAdmin,
        Instant createdAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return createdAt; }
    }

    public record ChatMembersAdded(
        long chatId,
        List<Long> userIds,
        List<Boolean> isAdmin,
        Instant createdAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return createdAt; }
    }

    public record ChatMemberInfoUpdate(
        long chatId,
        long userId,
        String tag,
        Instant updatedAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return updatedAt; }
    }

    public record ChatMemberAdminUpdated(
        long chatId,
        long userId,
        boolean isAdmin,
        Instant updatedAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return updatedAt; }
    }

    public record ChatMemberRemoved(
        long chatId,
        long userId,
        Instant removedAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return removedAt; }
    }


    // ==================== MESSAGE ====================

    public record MessageCreatedFull(
        String tempId,
        long chatId,
        long messageId,
        long senderId,
        String text,
        Instant userProfileUpdatedAt,
        Instant createdAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return createdAt; }
    }

    public record MessageCreated(
        String tempId,
        long chatId,
        long messageId,
        Instant createdAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return createdAt; }
    }

    public record MessageUpdated(
        long chatId,
        long messageId,
        String newText,
        Instant updatedAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return updatedAt; }
    }

    public record MessageDeleted(
        long chatId,
        long messageId,
        Instant deletedAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return deletedAt; }
    }

    public record MessagesReadUpTo(
        long chatId,
        long userId,
        long upToMessageId,
        Instant readAt
    ) implements IDomainEvent {
        @Override public Instant getCreatedAt() { return readAt; }
    }
}