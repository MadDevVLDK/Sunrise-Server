package com.sunrise.core.service;

import com.sunrise.core.creation.CreateDto;
import com.sunrise.helpclass.SnowflakeId;
import com.sunrise.helpclass.exception.MyErrorCode;
import com.sunrise.helpclass.exception.MyException;
import com.sunrise.orchestrator.DataValidator;
import com.sunrise.orchestrator.result.Dto.*;
import com.sunrise.orchestrator.service.MessageOrchestrator;
import com.sunrise.orchestrator.service.UserOrchestrator;
import com.sunrise.orchestrator.type.Direction;
import com.sunrise.orchestrator.type.MessageType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageOrchestrator messageOrchestrator;
    private final UserOrchestrator userOrchestrator;

    private final DataValidator validator;

    // TODO: Посмотреть на счет валидации в сервисе, может быть стоит вынести в контроллеры, чтобы не дублировать в разных сервисах
    @Transactional
    public Long makePublicMessage(String tempId, long chatId, long senderId, String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new MyException(
                MyErrorCode.MESSAGE_EMPTY,
                "Message text cannot be empty in chat " + chatId
            );
        }
        if (text.length() > 10000) {
            throw new MyException(
                MyErrorCode.MESSAGE_TOO_LONG,
                "Message text is too long (" + text.length() + " chars, max 10000) in chat " + chatId
            );
        }

        validator.validateActiveChatMemberInActiveChat(chatId, senderId);

        UserProfileLight user = userOrchestrator.getUserProfileLight(senderId)
            .orElseThrow(() -> new MyException(
                MyErrorCode.USER_NOT_FOUND_OR_DELETED,
                "Sender not found or is deleted -> " + senderId
            ));

        CreateDto.Message message = new CreateDto.Message(
            SnowflakeId.next(), chatId, senderId,
            MessageType.COMMON, text, Instant.now()
        );

        boolean isSaved = messageOrchestrator.save(tempId, user.profileUpdatedAt(), message);
        if (!isSaved) {
            throw new MyException(
                MyErrorCode.INTERNAL_ERROR,
                "Failed to save message in chat " + chatId
            );
        }

        log.info("[🔧] ✅ User {} send public message {} in chat {}", senderId, message.getId(), chatId);
        return message.getId();
    }

    @Transactional
    public void updateMessage(long chatId, long userId, long messageId, String newText) {
        validator.validateCanUpdateMessage(chatId, userId, messageId);

        Instant updatedAt = Instant.now();
        boolean isUpdated = messageOrchestrator.update(chatId, messageId, newText, updatedAt);
        if (isUpdated) {
            log.info("[🔧] ✅ User {} updated message {} in chat {}", userId, messageId, chatId);
        }
    }

    @Transactional
    public void deleteMessage(long chatId, long userId, long messageId) {
        validator.validateCanDeleteMessage(chatId, userId, messageId);

        Instant updatedAt = Instant.now();
        boolean isUpdated = messageOrchestrator.delete(chatId, messageId, updatedAt);
        if (isUpdated) {
            log.info("[🔧] ✅ User {} deleted message {} in chat {}", userId, messageId, chatId);
        }
    }

    @Transactional
    public void markMessagesUpToRead(long chatId, long userId, long messageId) {
        validator.validateActiveChatMemberInActiveChat(chatId, userId);

        Instant readAt = Instant.now();
        boolean isUpdated = messageOrchestrator.markMessagesUpToRead(chatId, userId, messageId, readAt);
        if (isUpdated) {
            log.info("[🔧] ✅ User {} marked messages up to {} as read in chat {}", userId, messageId, chatId);
        }
    }

    @Transactional(readOnly = true)
    public Message getMessage(long chatId, long userId, long messageId) {
        validator.validateActiveChatMemberInActiveChat(chatId, userId);

        Message message = messageOrchestrator.getActiveWithReadStatusInChat(chatId, userId, messageId)
            .orElseThrow(() -> new MyException(
                MyErrorCode.MESSAGE_NOT_FOUND_OR_DELETED,
                "Message " + messageId + " not found or is deleted in chat " + chatId
            ));

        log.info("[🔧] ✅ User {} got message {} in chat {}", userId, messageId, chatId);
        return message;
    }

    @Transactional(readOnly = true)
    public List<Message> getMessageBatch(long chatId, long userId, Set<Long> messageIds) {
        validator.validateActiveChatMemberInActiveChat(chatId, userId);

        List<Message> messages = messageOrchestrator.getActiveWithReadStatusInChatBatch(chatId, userId, messageIds);
        log.info("[🔧] ✅ User {} got {} messages in chat {}", userId, messages.size(), chatId);
        return messages;
    }

    @Transactional(readOnly = true)
    public MessagesPage getMessagePagination(long chatId, long userId, Long cursor, int limit, Direction direction) {
        validator.validateActiveChatMemberInActiveChat(chatId, userId);

        MessagesPage pagination = messageOrchestrator.getPage(chatId, userId, cursor, limit, direction);
        log.info("[🔧] ✅ User {} got {} messages in chat {}", userId, pagination.messages().size(), chatId);
        return pagination;
    }

    @Transactional(readOnly = true)
    public List<MessageReadStatus> getMessageReads(long chatId, long userId, long messageId) {
        validator.validateActiveChatMemberInActiveChat(chatId, userId);
        validator.validateActiveMessageInChat(chatId, messageId);

        List<MessageReadStatus> reads = messageOrchestrator.getMessageReaders(messageId);
        log.info("[🔧] ✅ User {} got {} reads of message {} in chat {}", userId, reads.size(), messageId, chatId);
        return reads;
    }
}