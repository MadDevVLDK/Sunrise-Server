package com.sunrise.cache.event;

import com.sunrise.cache.event.CacheEvent.*;
import com.sunrise.cache.service.*;
import com.sunrise.db.service.ChatMemberDbService;
import com.sunrise.db.service.MessageDbService;
import com.sunrise.web.websocket.service.UserGlobalStatusKeeper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheEventListener {

    private final UserCacheService userCache;
    private final ChatCacheService chatCache;
    private final ChatMemberCacheService chatMemberCache;
    private final MessageCacheService messageCache;
    private final VerificationTokenCacheService verificationTokenCache;

    private final MessageDbService messageDb;
    private final ChatMemberDbService chatMemberDb;

    private final UserGlobalStatusKeeper userGlobalStatusKeeper;


    // ========================= USER ===============================

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(UserCreated event) {
        try {
            userCache.saveProfile(event.profile());
            userCache.saveSecurity(event.security());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache user after commit: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserProfileSave(UserProfileSave event) {
        try {
            userCache.saveProfile(event.profile());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache user profile after commit: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserProfilesSave(UserProfilesSave event) {
        try {
            userCache.saveProfiles(event.profiles());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache user profiles after commit: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserSecuritySave(UserSecuritySave event) {
        try {
            userCache.saveSecurity(event.security());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache user security after commit: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserProfileUpdated(UserProfileUpdated event) {
        try {
            userCache.invalidateProfileAndUsernameIndex(event.userId(), event.oldUsername());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate user profile cache: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserEmailUpdated(UserEmailUpdated event) {
        try {
            userCache.invalidateSecurityAndEmailIndex(event.userId(), event.oldEmail());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate user email cache: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserSecurityInvalidation(UserSecurityAndProfileInvalidated event) {
        try {
            userCache.invalidateProfile(event.userId());
            userCache.invalidateSecurity(event.userId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate user profile and security cache: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserSecurityInvalidation(UserSecurityInvalidated event) {
        try {
            userCache.invalidateSecurity(event.userId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate user security cache: {}", e.getMessage());
        }
    }


    // ====================== CHAT ===============================

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatCreated(ChatWithMembersCreated event) {
        try {
            chatCache.save(event.chat());
            chatMemberCache.saveBatch(event.chat().getId(), event.members());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache created chat: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatSave(ChatSave event) {
        try {
            chatCache.save(event.chat());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache chat: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatsSave(ChatsSave event) {
        try {
            chatCache.saveBatch(event.chats());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache chats: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatInvalidation(ChatInvalidated event) {
        try {
            chatCache.invalidate(event.chatId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate chat cache: {}", e.getMessage());
        }
    }


    // ====================== CHAT-MEMBER =========================

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMemberAdded(ChatMemberAdded event) {
        try {
            var member = event.member();
            chatMemberCache.save(member);
            chatCache.increaseChatMemberCounter(member.chatId(), 1);
            chatMemberCache.addToRecentIds(member.chatId(), member.userId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache chat member: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMembersAdded(ChatMembersAdded event) {
        try {
            chatMemberCache.saveBatch(event.chatId(), event.members());
            chatCache.increaseChatMemberCounter(event.chatId(), event.members().size());
            chatMemberCache.invalidateResentIds(event.chatId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache chat members: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMemberSaved(ChatMemberSave event) {
        try {
            chatMemberCache.save(event.member());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache chat member: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMembersSaved(ChatMembersSave event) {
        try {
            chatMemberCache.saveBatch(event.chatId(), event.members());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache chat members: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMemberRemoved(ChatMemberRemoved event) {
        try {
            chatMemberCache.invalidate(event.chatId(), event.userId());
            chatMemberCache.removeFromRecentIds(event.chatId(), event.userId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate chat member resent: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMembersIdsInit(ChatMembersIdsInit event) {
        try {
            long chatId = event.chatId();
            if (chatMemberCache.hasResentIds(event.chatId())) {
                return; // уже инициализирован
            }

            int limit = chatMemberCache.getMaxMembersResentIdsPerChat();
            List<Long> ids = chatMemberDb.getIdsPage(chatId, null, limit);
            chatMemberCache.saveResentIds(chatId, ids);
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to init member IDs cache for chat {}: {}", event.chatId(), e.getMessage());
        }
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMemberInvalidation(ChatMemberInvalidated event) {
        try {
            chatMemberCache.invalidate(event.chatId(), event.userId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate chat member: {}", e.getMessage());
        }
    }
    

    // ====================== MESSAGE ============================

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageCreated(MessageCreated event) {
        try {
            var message = event.message();
            messageCache.save(message);
            messageCache.addToRecentIds(message.chatId(), message.id());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache new message: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSaved(MessageSave event) {
        try {
            messageCache.save(event.message());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache message: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessagesSaved(MessagesSave event) {
        try {
            messageCache.saveBatch(event.messages());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache messages: {}", e.getMessage());
        }
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRecentMessagesIdsInit(MessagesRecentIdsInit event) {
        try {
            if (messageCache.hasRecentIds(event.chatId())) {
                return;
            }

            int limit = messageCache.getMaxMessagesResentIdsPerChat();
            List<Long> idsFromDb = messageDb.getLastMessageIds(event.chatId(), limit);
            messageCache.saveRecentIds(event.chatId(), idsFromDb);
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to init cache of recent chat messages: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageMarkAsReadBatch(MessagesMarkAsReadBatch event) {
        try {
            messageCache.markAsReadBatch(event.messageIds());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to increment readCount for messages: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageInvalidation(MessageInvalidated event) {
        try {
            messageCache.invalidate(event.messageId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate message: {}", e.getMessage());
        }
    }


    // ================== VERIFICATION-TOKEN =======================

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationTokenCreated(VerificationTokenCreated event) {
        try {
            verificationTokenCache.save(event.token());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache verification token: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationTokenCreated(VerificationTokenSave event) {
        try {
            verificationTokenCache.save(event.token());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to cache verification token: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationTokenDeleted(VerificationTokenDeleted event) {
        try {
            verificationTokenCache.invalidate(event.token());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to invalidate verification token: {}", e.getMessage());
        }
    }


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCleanUserChatAction(CleanUserChatAction event) {
        try {
            userGlobalStatusKeeper.removeUserActionFromChat(event.chatId(), event.userId());
            log.debug("[⚡] 🧹 Cleaned actions for user {} in chat {}", event.userId(), event.chatId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to clean user actions: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCleanChatActions(CleanChatActions event) {
        try {
            userGlobalStatusKeeper.removeChatActions(event.chatId());
            log.debug("[⚡] 🧹 Cleaned all actions for chat {}", event.chatId());
        } catch (Exception e) {
            log.error("[⚡] ❌ Failed to clean chat actions: {}", e.getMessage());
        }
    }
}