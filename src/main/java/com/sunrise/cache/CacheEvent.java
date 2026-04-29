package com.sunrise.cache;

import com.sunrise.cache.entity.*;

import java.util.List;
public final class CacheEvent {

    // USER
    public record UserCreated(CacheUserProfile profile, CacheUserSecurity security) {}
    public record UserProfileSave(CacheUserProfile profile) {}
    public record UserProfilesSave(List<CacheUserProfile> profiles) {}
    public record UserSecuritySave(CacheUserSecurity security) {}
    public record UserProfileUpdated(long userId, String oldUsername, String username) {}
    public record UserEmailUpdated(long userId, String oldEmail, String email) {}
    public record UserSecurityInvalidated(long userId) {}
    public record UserSecurityAndProfileInvalidated(long userId) {}

    // CHAT
    public record ChatWithMembersCreated(CacheChat chat, List<CacheChatMember> members) {}
    public record ChatSave(CacheChat chat) {}
    public record ChatsSave(List<CacheChat> chats) {}
    public record ChatInvalidated(long chatId) {}

    // CHAT-MEMBER
    public record ChatMemberAdded(CacheChatMember member) {}
    public record ChatMembersAdded(long chatId, List<CacheChatMember> members) {}
    public record ChatMemberSave(CacheChatMember member) {}
    public record ChatMembersSave(long chatId, List<CacheChatMember> members) {}
    public record ChatMembersIdsInit(long chatId) { }
    public record ChatMemberInvalidated(long chatId, long userId) {}

    // MESSAGE
    public record MessageCreated(CacheMessage message) {}
    public record MessageSave(CacheMessage message) {}
    public record MessagesSave(List<CacheMessage> messages) {}
    public record MessagesRecentIdsInit(long chatId) {}
    public record MessageInvalidated(long messageId) {}

    // VERIFICATION-TOKEN
    public record VerificationTokenCreated(CacheVerificationToken token) {}
    public record VerificationTokenSave(CacheVerificationToken token) {}
    public record VerificationTokenDeleted(String token) {}
}