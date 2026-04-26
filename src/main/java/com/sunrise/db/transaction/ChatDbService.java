package com.sunrise.db.transaction;

import com.sunrise.db.DBService;
import com.sunrise.db.entity.Chat;
import com.sunrise.db.result.ChatProfileResult;
import com.sunrise.db.result.ChatStatsResult;
import com.sunrise.db.result.UserChatResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatDbService {

    private final DBService dbService;

    @Transactional
    public void saveGroupChat(Chat chat, Long[] membersWithoutCreatorIds) {
        dbService.saveGroupChat(chat, membersWithoutCreatorIds);
    }

    @Transactional
    public void savePersonalChat(Chat chat, long opponentId) {
        dbService.savePersonalChat(chat, opponentId);
    }

    @Transactional
    public int updateChatInfo(long chatId, String newName, String newDescription, LocalDateTime updatedAt) {
        return dbService.updateChatInfo(chatId, newName, newDescription, updatedAt);
    }

    @Transactional
    public int restoreChat(long chatId, LocalDateTime updatedAt) {
        return dbService.restoreChat(chatId, updatedAt);
    }

    @Transactional
    public int deleteChat(long chatId, LocalDateTime updatedAt) {
        return dbService.deleteChat(chatId, updatedAt);
    }

    @Transactional(readOnly = true)
    public Optional<ChatProfileResult> getChat(long chatId) {
        return dbService.getChat(chatId);
    }

    @Transactional(readOnly = true)
    public Optional<ChatProfileResult> getPersonalChat(long userId1, long userId2) {
        return dbService.getPersonalChat(userId1, userId2);
    }

    @Transactional(readOnly = true)
    public List<UserChatResult> getUserChatsPage(long userId, Boolean isPinnedCursor, Long lastMsgIdCursor, 
                                                 Long chatIdCursor, int limit) {
                                                    
        return dbService.getUserChatsPage(userId, isPinnedCursor, lastMsgIdCursor, chatIdCursor, limit);
    }

    @Transactional(readOnly = true)
    public Optional<UserChatResult> getUserChat(long chatId, long userId) {
        return dbService.getUserChat(chatId, userId);
    }

    @Transactional(readOnly = true)
    public List<Long> getUserChatIds(long userId) {
        return dbService.getUserChatIds(userId);
    }

    @Transactional(readOnly = true)
    public ChatStatsResult getChatClearStats(long chatId, long userId) {
        return dbService.getChatMessagesDeletedStats(chatId, userId);
    }
}