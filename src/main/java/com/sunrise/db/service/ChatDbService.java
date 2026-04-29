package com.sunrise.db.service;

import com.sunrise.db.entity.Chat;
import com.sunrise.db.repository.ChatRepository;
import com.sunrise.db.result.ChatMetaResult;
import com.sunrise.db.result.ChatProfileResult;
import com.sunrise.db.result.ChatStatsResult;
import com.sunrise.db.result.UserChatResult;
import com.sunrise.orchestrator.type.ChatType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatDbService {

    private final ChatRepository chatRepository;

    @Transactional
    public void saveGroupChat(Chat chat, Long[] membersWithoutCreatorIds) {
        log.debug("[🗄️] 💬 Saving group chat: id={}, name={}, membersCount={}", 
            chat.getId(), chat.getName(), membersWithoutCreatorIds.length + 1);
        chatRepository.saveGroupChatAndMembers(
            chat.getId(), chat.getName(), chat.getDescription(),
            chat.getChatType().name(), membersWithoutCreatorIds,
            chat.getCreatedBy(), chat.getCreatedAt()
        );
    }

    @Transactional
    public void savePersonalChat(Chat chat, long opponentId) {
        log.debug("[🗄️] 💑 Saving personal chat: id={}, creator={}, opponent={}", 
            chat.getId(), chat.getCreatedBy(), opponentId);
        chatRepository.savePersonalChatAndMembers(
            chat.getId(),
            chat.getChatType().name(),
            chat.getCreatedBy(), opponentId,
            chat.getCreatedAt()
        );
    }

    @Transactional
    public int updateProfile(long chatId, String newName, String newDescription, Instant updatedAt) {
        log.debug("[🗄️] 📝 Updating chat info: id={}, newName={}", chatId, newName);
        int result = chatRepository.updateChatInfo(chatId, newName, newDescription, updatedAt);
        log.debug("[🗄️] 📝 Chat info updated: affectedRows={}", result);
        return result;
    }

    @Transactional
    public int restore(long chatId, Instant updatedAt) {
        log.debug("[🗄️] 🔄 Restoring chat: id={}", chatId);
        int result = chatRepository.restoreChat(chatId, updatedAt);
        log.debug("[🗄️] 🔄 Chat restored: affectedRows={}", result);
        return result;
    }

    @Transactional
    public int delete(long chatId, Instant updatedAt) {
        log.debug("[🗄️] 🗑️ Deleting chat: id={}", chatId);
        int result = chatRepository.deleteChat(chatId, updatedAt);
        log.debug("[🗄️] 🗑️ Chat deleted: affectedRows={}", result);
        return result;
    }

    @Transactional(readOnly = true)
    public Optional<ChatProfileResult> get(long chatId) {
        log.debug("[🗄️] 🔍 Getting chat by id: {}", chatId);
        return chatRepository.getChat(chatId);
    }

    @Transactional(readOnly = true)
    public Optional<ChatProfileResult> getPersonalChat(long userId1, long userId2) {
        log.debug("[🗄️] 🔍 Getting personal chat between {} and {}", userId1, userId2);
        return chatRepository.getPersonalChat(userId1, userId2, ChatType.PERSONAL);
    }

    @Transactional(readOnly = true)
    public Optional<UserChatResult> getUserChat(long chatId, long userId) {
        log.debug("[🗄️] 🔍 Getting user chat: chatId={}, userId={}", chatId, userId);
        return chatRepository.getUserChat(chatId, userId);
    }

    @Transactional(readOnly = true)
    public List<ChatMetaResult> getChatsMeta(long userId) {
        log.debug("[🗄️] 📊 Getting chat meta for user {}", userId);
        return chatRepository.getChatsMeta(userId);
    }

    @Transactional(readOnly = true)
    public List<UserChatResult> getChatsByIds(long userId, Long[] chatsIds) {
        log.debug("[🗄️] 📊 Getting chats by ids for user {}", userId);
        return chatRepository.getChatsByIds(userId, chatsIds);
    }

    @Transactional(readOnly = true)
    public ChatStatsResult getChatClearStats(long chatId, long userId) {
        log.debug("[🗄️] 📊 Getting chat clear stats: chatId={}, userId={}", chatId, userId);
        return chatRepository.getChatClearStats(chatId, userId);
    }
}