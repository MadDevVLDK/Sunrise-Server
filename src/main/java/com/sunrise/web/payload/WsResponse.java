package com.sunrise.web.payload;

import java.time.Instant;

import com.sunrise.orchestrator.type.ChatType;

public final class WsResponse {

    // ---------- Server -> Client ----------
    public record MessageNew(long tempId, long messageId, long chatId, long senderId, Instant senderProfileUpdatedAt, String text, long readCount, Instant sentAt, Instant updatedAt, Instant deletedAt, boolean isDeleted) {}
    public record MessagePrivateNew(long tempId, long messageId, long chatId, long senderId, Instant senderProfileUpdatedAt, String text, Instant sentAt) {}
    public record MessageUpdate(long messageId, long chatId, String newText, Instant updatedAt) {}
    public record MessageDelete(long messageId, long chatId, Instant deletedAt) {}

    public record MessagesReadUpTo(long userId, long chatId, long upToMessageId, Instant readAt) {}

    public record ChatNew(long tempId, long chatId, String name, String description, ChatType chatType, Long opponentId, int membersCount, Instant updatedAt, Instant createdAt, long createdBy) {}
    public record ChatInfoUpdate(long chatId, String newName, String newDescription, Instant updatedAt) {}
    public record ChatDelete(long chatId, Instant deletedAt) {}

    public record ChatMemberNew(long chatId, long userId, Instant updatedAt, Instant joinedAt, boolean isAdmin) {}
    public record ChatMemberInfoUpdate(long chatId, long userId, String tag, Instant updatedAt) {}
    public record ChatMemberAdminRightsUpdate(long chatId, long userId, boolean isAdmin, Instant updatedAt) {}
    public record SelfChatSettingsUpdate(long chatId, boolean isPinned, Instant updatedAt) {}
    public record ChatMemberDelete(long chatId, long userId, Instant deletedAt) {}

    public record UserStatus(long userId, String newStatus) {}
    public record UserChatAction(long userId, long chatId, String action) {}
    public record Pong() {}

    public record Error(String error, String message, String path) {}
}