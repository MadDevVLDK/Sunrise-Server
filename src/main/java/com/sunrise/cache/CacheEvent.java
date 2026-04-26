package com.sunrise.cache;

import com.sunrise.cache.entity.*;

import java.util.List;
public final class CacheEvent {

    // USER
    public record UserCreated(CacheUserProfile profile, CacheUserSecurity security) {}
    public record UserProfileUpdated(long userId, String oldUsername, String username) {}
    public record UserEmailUpdated(long userId, String oldEmail, String email) {}
    public record UserSecurityInvalidated(long userId) {}
    public record UserSecurityAndProfileInvalidated(long userId) {}

    // CHAT
    public record ChatCreated(CacheChat chat, List<CacheChatMember> members) {}
    public record ChatInvalidated(long chatId) {}

    // CHAT-MEMBER
    public record ChatMemberSaved(CacheChatMember member) {}
    public record ChatMembersSaved(long chatId, List<CacheChatMember> members) {}
    public record ChatMemberInvalidated(long chatId, long userId) {}

    // MESSAGE
    public record MessageCreated(CacheMessageSecurity message) {}
    public record MessageInvalidated(long messageId) {}

    // VERIFICATION-TOKEN
    public record VerificationTokenCreated(CacheVerificationToken token) {}
    public record VerificationTokenDeleted(String token) {}
}