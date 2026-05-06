package com.sunrise.cache.event;

import com.sunrise.cache.entity.Cache.*;

import java.util.List;
public final class CacheEvent {


    // ========================= USER ============================

    public record UserCreated(UserProfile profile, UserSecurity security) {}
    public record UserProfileSave(UserProfile profile) {}
    public record UserProfilesSave(List<UserProfile> profiles) {}
    public record UserSecuritySave(UserSecurity security) {}
    public record UserProfileUpdated(long userId, String oldUsername) {}
    public record UserEmailUpdated(long userId, String oldEmail) {}
    public record UserSecurityInvalidated(long userId) {}
    public record UserSecurityAndProfileInvalidated(long userId) {}


    // ========================= CHAT =============================

    public record ChatWithMembersCreated(Chat chat, List<ChatMember> members) {}
    public record ChatSave(Chat chat) {}
    public record ChatsSave(List<Chat> chats) {}
    public record ChatInvalidated(long chatId) {}

    
    // ====================== CHAT-MEMBER =========================

    public record ChatMemberAdded(ChatMember member) {}
    public record ChatMembersAdded(long chatId, List<ChatMember> members) {}
    public record ChatMemberSave(ChatMember member) {}
    public record ChatMembersSave(long chatId, List<ChatMember> members) {}
    public record ChatMemberRemoved(long chatId, long userId) {}
    public record ChatMembersIdsInit(long chatId) { }
    public record ChatMemberInvalidated(long chatId, long userId) {}


    // ======================= MESSAGE ===========================

    public record MessageCreated(Message message) {}
    public record MessageSave(Message message) {}
    public record MessagesSave(List<Message> messages) {}
    public record MessagesRecentIdsInit(long chatId) {}
    public record MessagesReadCountIncremented(List<Long> messageIds) {}
    public record MessageInvalidated(long messageId) {}


    // ================== VERIFICATION-TOKEN =====================

    public record VerificationTokenCreated(VerificationToken token) {}
    public record VerificationTokenSave(VerificationToken token) {}
    public record VerificationTokenDeleted(String token) {}


    // ===================== USER-ACTIONS ========================
    public record CleanUserChatAction(long chatId, long userId) {}
    public record CleanChatActions(long chatId) {}
}