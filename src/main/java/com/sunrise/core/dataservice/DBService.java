package com.sunrise.core.dataservice;

import com.sunrise.core.dataservice.dbresult.*;
import com.sunrise.core.dataservice.type.*;
import com.sunrise.core.dataservice.type.Direction;
import com.sunrise.entity.db.*;
import com.sunrise.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DBService {

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final VerificationTokenRepository tokenRepository;
    private final MessageRepository messageRepository;

    public DBService(UserRepository userRepository, ChatRepository chatRepository, LoginHistoryRepository loginHistoryRepository,
                     VerificationTokenRepository tokenRepository, MessageRepository messageRepository, ChatMemberRepository chatMemberRepository) {
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.tokenRepository = tokenRepository;
        this.messageRepository = messageRepository;
        this.chatMemberRepository = chatMemberRepository;
    }


    // ========== USER METHODS ==========


    // Основные методы
    public void saveUser(User user) {
        userRepository.save(user);
    }
    @Async("dbExecutor")
    public void updateLastLoginAsync(String username, LocalDateTime lastLogin) {
        userRepository.updateLastLogin(username, lastLogin);
    }
    public int updateUserProfile(long userId, String username, String name, LocalDateTime updatedAt) {
        return userRepository.updateProfile(userId, username, name, updatedAt);
    }
    public int updateUserEmail(long userId, String email, LocalDateTime updatedAt) {
        return userRepository.updateUserEmail(userId, email, updatedAt);
    }
    public int updateUserPassword(long userId, String password, LocalDateTime updatedAt) {
        return userRepository.updateUserPassword(userId, password, updatedAt);
    }
    public int enableUser(long userId, LocalDateTime updatedAt) {
        return userRepository.enableUser(userId, updatedAt);
    }
    public int disableUser(long userId, LocalDateTime updatedAt) {
        return userRepository.disableUser(userId, updatedAt);
    }
    public int deleteUser(long userId, LocalDateTime updatedAt) {
        return userRepository.deleteUser(userId, updatedAt);
    }
    public int restoreUser(long userId, LocalDateTime updatedAt) {
        return userRepository.restoreUser(userId, updatedAt);
    }


    // Вспомогательные методы
    public Optional<UserSecurityResult> getUserSecurity(long userId) {
        return userRepository.getUserSecurity(userId);
    }
    public Optional<UserSecurityResult> getUserSecurityByUsername(String username) {
        return userRepository.getUserSecurityByUsername(username);
    }
    public Optional<UserSecurityResult> getUserSecurityByEmail(String email) {
        return userRepository.getUserSecurityByEmail(email);
    }

    public Optional<UserProfileResult> getUserProfile(long userId) {
        return userRepository.getUserProfile(userId);
    }
    public List<UserProfileResult> getActiveUserProfileByIds(List<Long> missingIds) {
        return userRepository.getActiveUserProfileByIds(missingIds);
    }
    public List<UserProfileResult> getActiveUsersPage(String filter, Long cursor, int limit) {
        return userRepository.getActiveUsersPage(filter, cursor, Pageable.ofSize(limit));
    }


    // ========== CHAT METHODS ==========


    // Основные методы
    public void saveGroupChat(Chat chat, Long[] membersWithoutCreatorIds) {
        chatRepository.saveGroupChatAndMembers(
            chat.getId(), chat.getName(), chat.getDescription(),
            chat.getChatType().name(), membersWithoutCreatorIds,
            chat.getCreatedBy(), chat.getCreatedAt()
        );
    }
    public void savePersonalChat(Chat chat, long opponentId) {
        chatRepository.savePersonalChatAndMembers(
            chat.getId(),
            chat.getChatType().name(),
            chat.getCreatedBy(), opponentId,
            chat.getCreatedAt()
        );
    }

    public int updateChatInfo(long chatId, String newName, String newDescription, LocalDateTime updatedAt) {
        return chatRepository.updateChatInfo(chatId, newName, newDescription, updatedAt);
    }

    public int restoreChat(long chatId, LocalDateTime updatedAt) {
        return chatRepository.restoreChat(chatId, updatedAt);
    }
    public int deleteChat(long chatId, LocalDateTime updatedAt) {
        return chatRepository.deleteChat(chatId, updatedAt);
    }


    // Вспомогательные методы
    public Optional<ChatProfileResult> getChat(long chatId) {
        return chatRepository.getChat(chatId);
    }
    public Optional<ChatProfileResult> getPersonalChat(long userId1, long userId2) {
        return chatRepository.getPersonalChat(userId1, userId2, ChatType.PERSONAL);
    }
    public List<UserChatResult> getUserChatsPage(long userId, Boolean isPinnedCursor, Long lastMsgIdCursor, Long chatIdCursor, int limit) {
        return chatRepository.getUserChatsPage(userId, isPinnedCursor, lastMsgIdCursor, chatIdCursor, limit);
    }
    public Optional<UserChatResult> getUserChat(long chatId, long userId) {
        return chatRepository.getUserChat(chatId, userId);
    }
    public List<Long> getUserChatIds(long userId) {
        return chatRepository.getUserChatIds(userId);
    }


    // ========== CHAT MEMBER METHODS ==========


    // Основные методы
    public void upsertChatMember(ChatMember chatMember) {
        chatMemberRepository.saveOrRestore(chatMember.getChatId(), chatMember.getUserId(), chatMember.isAdmin(), chatMember.getJoinedAt());
    }
    public void upsertChatMembers(long chatId, Long[] memberIds, LocalDateTime joinedAt) {
        chatMemberRepository.saveOrRestoreBatch(chatId, memberIds, joinedAt);
    }
    public int updateChatMemberInfo(long chatId, long userId, String tag, LocalDateTime updatedAt) {
        return chatMemberRepository.updateInfo(chatId, userId, tag, updatedAt);
    }
    public int updateChatMemberAdminRights(long chatId, long userId, boolean isAdmin, LocalDateTime updatedAt) {
        return chatMemberRepository.updateAdminRights(chatId, userId, isAdmin, updatedAt);
    }
    public int updateChatMemberSettings(long chatId, long userId, boolean isPinned, LocalDateTime updatedAt) {
        return chatMemberRepository.updateSettings(chatId, userId, isPinned, updatedAt);
    }
    public boolean removeChatMember(long userId, long chatId, LocalDateTime updatedAt) {
        return chatMemberRepository.remove(chatId, userId, updatedAt);
    }


    // Вспомогательные методы
    public Optional<ChatMember> getChatMember(long chatId, long userId) {
        return chatMemberRepository.findById(new ChatMemberId(chatId, userId));
    }
    public Optional<ChatMember> getActiveChatMember(long chatId, long userId) {
        return chatMemberRepository.getActive(chatId, userId);
    }
    public List<ChatMember> getActiveChatMembersByIds(long chatId, List<Long> missingIds) {
        return chatMemberRepository.getActiveByIds(chatId, missingIds);
    }

    public List<Long> getChatMemberIdsPage(long chatId, Long cursor, int limit) {
        return chatMemberRepository.getIdsPage(chatId, cursor, PageRequest.of(0, limit));
    }


    // ========== VERIFICATION TOKEN METHODS ==========


    // Основные методы
    @Async("dbExecutor")
    public void saveVerificationTokenAsync(VerificationToken token) {
        tokenRepository.save(token);
    }
    @Async("dbExecutor")
    public void deleteVerificationTokenAsync(String token) {
        tokenRepository.deleteByToken(token);
    }
    public Optional<VerificationToken> getVerificationToken(String token) {
        return tokenRepository.getByToken(token);
    }
    public int cleanupExpiredVerificationTokens() {
        return tokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }


    // ========== LOGIN HISTORY METHODS ==========


    // Основные методы
    @Async("dbExecutor")
    public void saveLoginHistoryAsync(LoginHistory loginHistory) {
        loginHistoryRepository.save(loginHistory);
    }


    // =========== MESSAGE METHODS ==========


    // Основные методы
    public void saveMessage(Message message) {
        messageRepository.save(message);
    }
    public void markMessagesUpToRead(long chatId, long userId, long messageId, LocalDateTime readAt) {
        messageRepository.markMessagesUpToRead(chatId, userId, messageId, readAt);
    }
    public int updateMessage(long messageId, String newText, LocalDateTime updatedAt) {
        return messageRepository.updateMessage(messageId, newText, updatedAt);
    }
    public int restoreMessage(long messageId, LocalDateTime updatedAt) {
        return messageRepository.restoreMessage(messageId, updatedAt);
    }
    public int deleteMessage(long messageId, LocalDateTime updatedAt) {
        return messageRepository.deleteMessage(messageId, updatedAt);
    }

    // Вспомогательные методы
    public List<UserMessageResult> getUserMessagePage(long chatId, long userId, Long cursor, int limit, Direction direction) {
        if (cursor == null) {
            return messageRepository.getFirstUserMessagePage(chatId, userId, PageRequest.of(0, limit));
        }
        if (direction == Direction.FORWARD) {
            return messageRepository.getUserMessagePageAfter(chatId, userId, cursor, PageRequest.of(0, limit));
        }

        return messageRepository.getUserMessagePageBefore(chatId, userId, cursor, PageRequest.of(0, limit));
    }

    public ChatStatsResult getChatMessagesDeletedStats(long chatId, long userId) {
        return chatRepository.getChatClearStats(chatId, userId);
    }

    public Optional<Message> getMessage(long chatId, long messageId) {
        return messageRepository.getMessageById(chatId, messageId);
    }
    public Optional<UserMessageResult> getUserMessage(long chatId, long userId, long messageId) {
        return messageRepository.getUserMessageById(chatId, userId, messageId);
    }
    public List<MessageReadStatusResult> getMessageReaders(long messageId){
        return messageRepository.getMessageReaders(messageId);
    }
}