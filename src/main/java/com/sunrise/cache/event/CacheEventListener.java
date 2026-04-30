package com.sunrise.cache.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sunrise.cache.entity.Cache.ChatMember;
import com.sunrise.cache.entity.Cache.Message;
import com.sunrise.cache.event.CacheEvent.MessagesRecentIdsInit;
import com.sunrise.cache.service.ChatCacheService;
import com.sunrise.cache.service.ChatMemberCacheService;
import com.sunrise.cache.service.MessageCacheService;
import com.sunrise.cache.service.UserCacheService;
import com.sunrise.cache.service.VerificationTokenCacheService;
import com.sunrise.db.service.ChatMemberDbService;
import com.sunrise.db.service.MessageDbService;


@Slf4j
@Component
@RequiredArgsConstructor
public class CacheEventListener {

    private final UserCacheService userCacheService;
    private final ChatCacheService chatCacheService;
    private final ChatMemberCacheService chatMemberCacheService;
    private final MessageCacheService messageCacheService;
    private final VerificationTokenCacheService verificationTokenCacheService;

    private final MessageDbService messageDbService;
    private final ChatMemberDbService chatMemberDbService;


    // ===== USER =====

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(CacheEvent.UserCreated event) {
        try {
            userCacheService.saveProfile(event.profile());
            userCacheService.saveSecurity(event.security());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache user after commit: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserProfileSave(CacheEvent.UserProfileSave event) {
        try {
            userCacheService.saveProfile(event.profile());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache user profile after commit: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserProfilesSave(CacheEvent.UserProfilesSave event) {
        try {
            userCacheService.saveProfiles(event.profiles());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache user profiles after commit: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserSecuritySave(CacheEvent.UserSecuritySave event) {
        try {
            userCacheService.saveSecurity(event.security());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache user security after commit: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserProfileUpdated(CacheEvent.UserProfileUpdated event) {
        try {
            userCacheService.invalidateProfileAndUsernameIndex(event.userId(), event.oldUsername());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate user profile cache: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserEmailUpdated(CacheEvent.UserEmailUpdated event) {
        try {
            userCacheService.invalidateSecurityAndEmailIndex(event.userId(), event.oldEmail());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate user email cache: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserSecurityInvalidation(CacheEvent.UserSecurityAndProfileInvalidated event) {
        try {
            userCacheService.invalidateProfile(event.userId());
            userCacheService.invalidateSecurity(event.userId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate user profile and security cache: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserSecurityInvalidation(CacheEvent.UserSecurityInvalidated event) {
        try {
            userCacheService.invalidateSecurity(event.userId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate user security cache: {}", e.getMessage());
        }
    }


    // ===== CHAT =====

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatCreated(CacheEvent.ChatWithMembersCreated event) {
        try {
            chatCacheService.save(event.chat());
            chatMemberCacheService.saveBatch(event.chat().getId(), event.members());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache created chat: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatSave(CacheEvent.ChatSave event) {
        try {
            chatCacheService.save(event.chat());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache chat: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatsSave(CacheEvent.ChatsSave event) {
        try {
            chatCacheService.saveBatch(event.chats());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache chats: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatInvalidation(CacheEvent.ChatInvalidated event) {
        try {
            chatCacheService.invalidate(event.chatId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate chat cache: {}", e.getMessage());
        }
    }


    // ===== CHAT MEMBER =====

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMemberAdded(CacheEvent.ChatMemberAdded event) {
        try {
            ChatMember member = event.member();
            chatMemberCacheService.save(member);
            chatCacheService.increaseChatMemberCounter(member.chatId(), 1);
            chatMemberCacheService.addToRecentIds(member.chatId(), member.userId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache chat member: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMembersAdded(CacheEvent.ChatMembersAdded event) {
        try {
            chatMemberCacheService.saveBatch(event.chatId(), event.members());
            chatCacheService.increaseChatMemberCounter(event.chatId(), event.members().size());
            chatMemberCacheService.invalidateResentIds(event.chatId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache chat members: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMemberSaved(CacheEvent.ChatMemberSave event) {
        try {
            chatMemberCacheService.save(event.member());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache chat member: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMembersSaved(CacheEvent.ChatMembersSave event) {
        try {
            chatMemberCacheService.saveBatch(event.chatId(), event.members());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache chat members: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMembersIdsInit(CacheEvent.ChatMembersIdsInit event) {
        try {
            long chatId = event.chatId();
            if (chatMemberCacheService.hasResentIds(event.chatId())) {
                return; // уже инициализирован
            }

            // Загружаем до limit последних ID участников
            int limit = chatMemberCacheService.getMaxMembersResentIdsPerChat();
            List<Long> ids = chatMemberDbService.getIdsPage(chatId, null, limit);
            
            // сохраняем
            chatMemberCacheService.saveResentIds(chatId, ids);
            
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to init member IDs cache for chat {}: {}", event.chatId(), e.getMessage());
        }
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMemberInvalidation(CacheEvent.ChatMemberInvalidated event) {
        try {
            chatMemberCacheService.invalidate(event.chatId(), event.userId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate chat member: {}", e.getMessage());
        }
    }


    // ===== MESSAGE =====

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageCreated(CacheEvent.MessageCreated event) {
        try {
            Message message = event.message();
            messageCacheService.save(message);
            messageCacheService.addToRecentIds(message.chatId(), message.id());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache new message: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSaved(CacheEvent.MessageSave event) {
        try {
            messageCacheService.save(event.message());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache message: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessagesSaved(CacheEvent.MessagesSave event) {
        try {
            messageCacheService.saveBatch(event.messages());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache messages: {}", e.getMessage());
        }
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRecentMessagesIdsInit(MessagesRecentIdsInit event) {
        try {
            if (messageCacheService.hasRecentIds(event.chatId())) {
                return; // уже инициализирован
            }

            // загружаем последние N ID из БД
            int limit = messageCacheService.getMaxMessagesResentIdsPerChat();
            List<Long> idsFromDb = messageDbService.getLastMessageIds(event.chatId(), limit);
            
            // сохраняем
            messageCacheService.saveRecentIds(event.chatId(), idsFromDb);

        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to init cache of recent chat messages: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageInvalidation(CacheEvent.MessageInvalidated event) {
        try {
            messageCacheService.invalidate(event.messageId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate message: {}", e.getMessage());
        }
    }


    // ===== VERIFICATION TOKEN =====

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationTokenCreated(CacheEvent.VerificationTokenCreated event) {
        try {
            verificationTokenCacheService.save(event.token());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache verification token: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationTokenCreated(CacheEvent.VerificationTokenSave event) {
        try {
            verificationTokenCacheService.save(event.token());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache verification token: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationTokenDeleted(CacheEvent.VerificationTokenDeleted event) {
        try {
            verificationTokenCacheService.invalidate(event.token());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate verification token: {}", e.getMessage());
        }
    }
}