package com.sunrise.db.service;

import com.sunrise.db.entity.Message;
import com.sunrise.db.repository.MessageRepository;
import com.sunrise.db.result.MessageReadStatusResult;
import com.sunrise.db.result.UserMessageResult;
import com.sunrise.orchestrator.type.Direction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Slf4j
@Service
@RequiredArgsConstructor
public class MessageDbService {

    private final MessageRepository messageRepository;

    @Transactional
    public void save(Message message) {
        log.debug("[🗄️] 💬 Saving message: id={}, chatId={}, senderId={}", 
            message.getId(), message.getChatId(), message.getSenderId());
        messageRepository.save(message);
    }

    @Transactional
    public List<Long> markMessagesUpToRead(long chatId, long userId, long messageId, Instant readAt) {
        log.debug("[🗄️] 👁️ Marking messages up to {} as read: chatId={}, userId={}", messageId, chatId, userId);
        return messageRepository.markMessagesUpToRead(chatId, userId, messageId, readAt);
    }

    @Transactional
    public int update(long messageId, String newText, Instant updatedAt) {
        log.debug("[🗄️] ✏️ Updating message: id={}", messageId);
        int result = messageRepository.update(messageId, newText, updatedAt);
        log.debug("[🗄️] ✏️ Message updated: affectedRows={}", result);
        return result;
    }

    @Transactional
    public int restore(long messageId, Instant updatedAt) {
        log.debug("[🗄️] 🔄 Restoring message: id={}", messageId);
        int result = messageRepository.restore(messageId, updatedAt);
        log.debug("[🗄️] 🔄 Message restored: affectedRows={}", result);
        return result;
    }

    @Transactional
    public int delete(long messageId, Instant updatedAt) {
        log.debug("[🗄️] 🗑️ Deleting message: id={}", messageId);
        int result = messageRepository.delete(messageId, updatedAt);
        log.debug("[🗄️] 🗑️ Message deleted: affectedRows={}", result);
        return result;
    }

    @Transactional(readOnly = true)
    public List<UserMessageResult> getPage(long chatId, long userId, Long cursor, int limit, Direction direction) {
        log.debug("[🗄️] 📄 Getting user message page: chatId={}, userId={}, cursor={}, limit={}, direction={}", 
            chatId, userId, cursor, limit, direction);
        
        if (cursor == null) {
            return messageRepository.getPageFirst(chatId, userId, PageRequest.of(0, limit));
        }
        if (direction == Direction.FORWARD) {
            return messageRepository.getPageAfter(chatId, userId, cursor, PageRequest.of(0, limit));
        }
        return messageRepository.getPageBefore(chatId, userId, cursor, PageRequest.of(0, limit));
    }
    

    @Transactional(readOnly = true)
    public Optional<Message> get(long chatId, long messageId) {
        log.debug("[🗄️] 🔍 Getting message: chatId={}, messageId={}", chatId, messageId);
        return messageRepository.get(chatId, messageId);
    }

    @Transactional(readOnly = true)
    public Optional<UserMessageResult> getUserMessage(long chatId, long userId, long messageId) {
        log.debug("[🗄️] 🔍 Getting user message: chatId={}, userId={}, messageId={}", chatId, userId, messageId);
        return messageRepository.getUserMessage(chatId, userId, messageId);
    }

    @Transactional(readOnly = true)
    public List<UserMessageResult> getUserMessageBatch(long chatId, long userId, Set<Long> messageIds) {
        log.debug("[🗄️] 🔍 Getting user {} messages: chatId={}, userId={}", messageIds.size(), chatId, userId);
        return messageRepository.getUserMessageBatch(chatId, userId, messageIds);
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesByIds(List<Long> ids) {
        return messageRepository.findAllById(ids);
    }

    @Transactional(readOnly = true)
    public List<Long> getLastMessageIds(long chatId, int limit) {
        return messageRepository.findLastMessageIds(chatId, PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<MessageReadStatusResult> getMessageReaders(long messageId) {
        log.debug("[🗄️] 🔍 Getting message readers for messageId={}", messageId);
        return messageRepository.getMessageReaders(messageId);
    }
}