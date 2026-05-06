package com.sunrise.core.service;

import com.sunrise.orchestrator.DataValidator;
import com.sunrise.orchestrator.result.Dto.*;
import com.sunrise.orchestrator.service.MessageOrchestrator;
import com.sunrise.orchestrator.service.UserOrchestrator;
import com.sunrise.orchestrator.type.Direction;
import com.sunrise.orchestrator.type.MessageType;
import com.sunrise.core.creation.CreateDto;
import com.sunrise.core.result.*;
import com.sunrise.helpclass.SnowflakeId;
import com.sunrise.helpclass.ValidationException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Slf4j
@Service
@AllArgsConstructor
public class MessageService {

    private final MessageOrchestrator messageOrchestrator;
    private final UserOrchestrator userOrchestrator;

    private final DataValidator validator;


    @Transactional
    public ResultOneArg<Long> makePublicMessage(String tempId, long chatId, long senderId, String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                throw new ValidationException("Message text cannot be empty");
            }

            if (text.length() > 10000) {
                throw new ValidationException("Message text is too long");
            }

            validator.validateActiveChatMemberInActiveChat(chatId, senderId);

            UserProfileLight user = userOrchestrator.getUserProfileLight(senderId)
                    .orElseThrow(() -> new ValidationException("User not found -> " + senderId));

            CreateDto.Message message = new CreateDto.Message(
                SnowflakeId.next(), chatId, senderId,
                MessageType.COMMON, text, Instant.now()
            );
            
            boolean isUpdated = messageOrchestrator.save(tempId, user.profileUpdatedAt(), message);
            if (!isUpdated) {
                throw new ValidationException("Failed to make message");
            }

            log.info("[🔧] ✅ User {} send public message {} in chat {}", senderId, message.getId(), chatId);
            return ResultOneArg.success(message.getId());
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed making public message: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error making public message: {}", e.getMessage(), e);
            return ResultOneArg.error("createPublicMessage failed due to server error");
        }
    }

    @Transactional
    public ResultNoArgs updateMessage(long chatId, long userId, long messageId, String newText) {
        try {
            validator.validateCanUpdateMessage(chatId, userId, messageId);

            Instant updatedAt = Instant.now();
            boolean isUpdated = messageOrchestrator.update(chatId, messageId, newText, updatedAt);
            if (isUpdated) {
                log.info("[🔧] ✅ User {} updated message {} in chat {}", userId, messageId, chatId);
            }
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed updating message: {}", e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error updating message: {}", e.getMessage());
            return ResultNoArgs.error("updateMessage failed due to server error");
        }
    }
    
    @Transactional
    public ResultNoArgs deleteMessage(long chatId, long userId, long messageId) {
        try {
            validator.validateCanDeleteMessage(chatId, userId, messageId);

            Instant updatedAt = Instant.now();
            boolean isUpdated = messageOrchestrator.delete(chatId, messageId, updatedAt);
            if (isUpdated) {
                log.info("[🔧] ✅ User {} deleted message {} in chat {}", userId, messageId, chatId);
            }
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed deleting message: {}", e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error deleting message: {}", e.getMessage());
            return ResultNoArgs.error("deleteMessage failed due to server error");
        }
    }

    @Transactional
    public ResultNoArgs markMessagesUpToRead(long chatId, long userId, long messageId) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            Instant readAt = Instant.now();
            boolean isUpdated = messageOrchestrator.markMessagesUpToRead(chatId, userId, messageId, readAt);
            if (isUpdated) {
                log.info("[🔧] ✅ User {} marked message as read {} in chat {}", userId, messageId, chatId);
            }
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed marking message as read: {}", e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error marking message as read: {}", e.getMessage());
            return ResultNoArgs.error("MarkMessageAsRead failed due to server error");
        }
    }

    @Transactional(readOnly = true)
    public ResultOneArg<Message> getMessage(long chatId, long userId, long messageId) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            Optional<Message> message = messageOrchestrator.getActiveWithReadStatusInChat(chatId, userId, messageId);
            if (message.isEmpty()) {
                throw new ValidationException("Message not found");
            }

            log.info("[🔧] ✅ User {} got message {} in chat {}", userId, messageId, chatId);
            return ResultOneArg.success(message.get());
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed getting message {} for user {} in chat {}: {}", userId, messageId, chatId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting message {} for user {} in chat {}: {}", userId, messageId, chatId, e.getMessage());
            return ResultOneArg.error("getMessage failed due to server error");
        }
    }

    @Transactional(readOnly = true)
    public ResultOneArg<List<Message>> getMessageBatch(long chatId, long userId, Set<Long> messageIds) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            List<Message> messages = messageOrchestrator.getActiveWithReadStatusInChatBatch(chatId, userId, messageIds);

            log.info("[🔧] ✅ User {} got {} messages in chat {}", userId, messages.size(), chatId);
            return ResultOneArg.success(messages);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed getting messages for user {} in chat {}: {}", userId, chatId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting message for user {} in chat {}: {}", userId, chatId, e.getMessage());
            return ResultOneArg.error("getMessage failed due to server error");
        }
    }

    @Transactional(readOnly = true)
    public ResultOneArg<MessagesPage> getMessagePagination(long chatId, long userId, Long cursor, int limit, Direction direction) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            MessagesPage pagination = messageOrchestrator.getPage(chatId, userId, cursor, limit, direction);

            log.info("[🔧] ✅ User {} got {} messages in chat {}", userId, pagination.messages().size(), chatId);
            return ResultOneArg.success(pagination);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed getting {} messages {}: {}", limit, direction.name(), e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting {} messages {}: {}", limit, direction.name(), e.getMessage());
            return ResultOneArg.error("getChatMessagesAfter failed due to server error");
        }
    }

    @Transactional(readOnly = true)
    public ResultOneArg<List<MessageReadStatus>> getMessageReads(long chatId, long userId, long messageId) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);
            validator.validateActiveMessageInChat(chatId, messageId);

            List<MessageReadStatus> message = messageOrchestrator.getMessageReaders(messageId);

            log.info("[🔧] ✅ User {} got {} reads of {} message in chat {}", userId, message.size(), messageId, chatId);
            return ResultOneArg.success(message);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed getting message reads {} for user {} in chat {}: {}", userId, messageId, chatId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting message reads {} for user {} in chat {}: {}", userId, messageId, chatId, e.getMessage());
            return ResultOneArg.error("getMessage failed due to server error");
        }
    }
}