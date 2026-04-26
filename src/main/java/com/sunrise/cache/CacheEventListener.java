package com.sunrise.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheEventListener {

    private final CacheService cacheService;


    // ===== USER =====

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(CacheEvent.UserCreated event) {
        try {
            cacheService.saveUserProfile(event.profile());
            cacheService.saveUserSecurity(event.security());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache user after commit: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserProfileUpdated(CacheEvent.UserProfileUpdated event) {
        try {
            cacheService.invalidateUserProfileAndUsernameIndex(event.userId(), event.oldUsername());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate user profile cache: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserEmailUpdated(CacheEvent.UserEmailUpdated event) {
        try {
            cacheService.invalidateUserSecurityAndEmailIndex(event.userId(), event.oldEmail());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate user email cache: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserSecurityInvalidation(CacheEvent.UserSecurityAndProfileInvalidated event) {
        try {
            cacheService.invalidateUserProfile(event.userId());
            cacheService.invalidateUserSecurity(event.userId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate user profile and security cache: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserSecurityInvalidation(CacheEvent.UserSecurityInvalidated event) {
        try {
            cacheService.invalidateUserSecurity(event.userId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate user security cache: {}", e.getMessage());
        }
    }



    // ===== CHAT =====

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatCreated(CacheEvent.ChatCreated event) {
        try {
            cacheService.saveChatAndAddMembers(event.chat(), event.members());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache created chat: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatInvalidation(CacheEvent.ChatInvalidated event) {
        try {
            cacheService.invalidateChat(event.chatId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate chat cache: {}", e.getMessage());
        }
    }


    // ===== CHAT MEMBER =====

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMemberSaved(CacheEvent.ChatMemberSaved event) {
        try {
            cacheService.saveChatMember(event.member());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache chat member: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMembersSaved(CacheEvent.ChatMembersSaved event) {
        try {
            cacheService.saveChatMembers(event.chatId(), event.members());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache chat members: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMemberInvalidation(CacheEvent.ChatMemberInvalidated event) {
        try {
            cacheService.invalidateChatMember(event.chatId(), event.userId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate chat member: {}", e.getMessage());
        }
    }


    // ===== MESSAGE =====

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageCreated(CacheEvent.MessageCreated event) {
        try {
            cacheService.saveMessage(event.message());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache message: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageInvalidation(CacheEvent.MessageInvalidated event) {
        try {
            cacheService.invalidateMessage(event.messageId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate message: {}", e.getMessage());
        }
    }


    // ===== VERIFICATION TOKEN =====

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationTokenCreated(CacheEvent.VerificationTokenCreated event) {
        try {
            cacheService.saveVerificationToken(event.token());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache verification token: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationTokenDeleted(CacheEvent.VerificationTokenDeleted event) {
        try {
            cacheService.invalidateVerificationToken(event.token());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate verification token: {}", e.getMessage());
        }
    }
}