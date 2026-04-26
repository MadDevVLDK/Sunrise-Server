package com.sunrise.db.transaction;

import com.sunrise.db.DBService;
import com.sunrise.db.entity.Message;
import com.sunrise.db.result.MessageReadStatusResult;
import com.sunrise.db.result.UserMessageResult;
import com.sunrise.dataservice.type.Direction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MessageDbService {

    private final DBService dbService;

    @Transactional
    public void saveMessage(Message message) {
        dbService.saveMessage(message);
    }

    @Transactional
    public void markMessagesUpToRead(long chatId, long userId, long messageId, LocalDateTime readAt) {
        dbService.markMessagesUpToRead(chatId, userId, messageId, readAt);
    }

    @Transactional
    public int updateMessage(long messageId, String newText, LocalDateTime updatedAt) {
        return dbService.updateMessage(messageId, newText, updatedAt);
    }

    @Transactional
    public int restoreMessage(long messageId, LocalDateTime updatedAt) {
        return dbService.restoreMessage(messageId, updatedAt);
    }

    @Transactional
    public int deleteMessage(long messageId, LocalDateTime updatedAt) {
        return dbService.deleteMessage(messageId, updatedAt);
    }

    @Transactional(readOnly = true)
    public List<UserMessageResult> getUserMessagePage(long chatId, long userId, Long cursor, int limit, Direction direction) {
        return dbService.getUserMessagePage(chatId, userId, cursor, limit, direction);
    }

    @Transactional(readOnly = true)
    public Optional<Message> getMessage(long chatId, long messageId) {
        return dbService.getMessage(chatId, messageId);
    }

    @Transactional(readOnly = true)
    public Optional<UserMessageResult> getUserMessage(long chatId, long userId, long messageId) {
        return dbService.getUserMessage(chatId, userId, messageId);
    }

    @Transactional(readOnly = true)
    public List<MessageReadStatusResult> getMessageReaders(long messageId) {
        return dbService.getMessageReaders(messageId);
    }
}