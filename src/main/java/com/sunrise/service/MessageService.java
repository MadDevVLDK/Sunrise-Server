package com.sunrise.service;

import com.sunrise.dataservice.type.MessageType;
import com.sunrise.notifier.WebSocketNotifier;
import com.sunrise.service.result.*;
import com.sunrise.dataservice.result.MessageReadStatusDTO;
import com.sunrise.dataservice.result.UserProfileLightDTO;
import com.sunrise.service.creation.CreateMessageDTO;
import com.sunrise.dataservice.result.MessagesPageDTO;
import com.sunrise.dataservice.result.UserMessageDTO;
import com.sunrise.dataservice.DataOrchestrator;
import com.sunrise.dataservice.type.Direction;
import com.sunrise.dataservice.DataValidator;
import com.sunrise.helpclass.SimpleSnowflakeId;
import com.sunrise.helpclass.ValidationException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class MessageService {

    private final DataValidator validator;
    private final DataOrchestrator dataOrchestrator;
    private final WebSocketNotifier wsNotify;

    public ResultOneArg<Long> makePublicMessage(long tempId, long chatId, long senderId, String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                throw new ValidationException("Message text cannot be empty");
            }

            if (text.length() > 10000) {
                throw new ValidationException("Message text is too long");
            }

            validator.validateActiveChatMemberInActiveChat(chatId, senderId);
            UserProfileLightDTO user = dataOrchestrator.getUserProfileLight(senderId)
                    .orElseThrow(() -> new ValidationException("User not found -> " + senderId));

            CreateMessageDTO message = new CreateMessageDTO(
                SimpleSnowflakeId.nextId(), chatId, senderId,
                MessageType.COMMON, text, LocalDateTime.now()
            );
            dataOrchestrator.saveMessage(message);

            // уведомить всех надо об этом
            wsNotify.notifyMessageNew(tempId, message, user.getProfileUpdatedAt());

            log.info("[🔧] ✅ User {} send public message {} in chat {}", senderId, message.getId(), chatId);
            return ResultOneArg.success(message.getId());
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed making public message: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error making public message: {}", e.getMessage());
            return ResultOneArg.error("createPublicMessage failed due to server error");
        }
    }
    public ResultOneArg<Long> makePrivateMessage(long tempId, long chatId, long senderId, long userToSend, String text) {
        try {
            // Валидация текста сообщения
            if (text == null || text.trim().isEmpty()) {
                throw new ValidationException("Message text cannot be empty");
            }

            if (text.length() > 10000) {
                throw new ValidationException("Message text is too long");
            }

            validator.validateActiveChatMemberInActiveChat(chatId, senderId);
            UserProfileLightDTO user = dataOrchestrator.getUserProfileLight(senderId)
                    .orElseThrow(() -> new ValidationException("User not found -> " + senderId));

            CreateMessageDTO message = new CreateMessageDTO(
                SimpleSnowflakeId.nextId(), chatId, senderId,
                MessageType.COMMON, text, LocalDateTime.now()
            );

            // уведомить всех надо об этом
            wsNotify.notifyMessagePrivateNew(tempId, message, user.getProfileUpdatedAt(), userToSend);

            log.info("[🔧] ✅ User {} send private message {} to user {} in chat {}", senderId, message.getId(), userToSend, chatId);
            return ResultOneArg.success(message.getId());
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed making private message: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error making private message: {}", e.getMessage());
            return ResultOneArg.error("createPrivateMessage failed due to server error");
        }
    }

    public ResultNoArgs updateMessage(long chatId, long userId, long messageId, String newText) {
        try {
            validator.validateCanUpdateMessage(chatId, userId, messageId);

            LocalDateTime updatedAt = LocalDateTime.now();
            dataOrchestrator.updateMessage(messageId, newText, updatedAt);

            // уведомить всех надо об этом
            wsNotify.notifyMessageInfoUpdated(chatId, messageId, newText, updatedAt);

            log.info("[🔧] ✅ User {} updated message {} in chat {}", userId, messageId, chatId);
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
    public ResultNoArgs deleteMessage(long chatId, long userId, long messageId) {
        try {
            validator.validateCanDeleteMessage(chatId, userId, messageId);

            LocalDateTime updatedAt = LocalDateTime.now();
            dataOrchestrator.deleteMessage(messageId, updatedAt);

            // уведомить всех надо об этом
            wsNotify.notifyMessageDeleted(chatId, messageId, updatedAt);

            log.info("[🔧] ✅ User {} deleted message {} in chat {}", userId, messageId, chatId);
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

    public ResultNoArgs markMessagesUpToRead(long chatId, long userId, long messageId) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            LocalDateTime readAt = LocalDateTime.now();
            dataOrchestrator.markMessagesUpToRead(chatId, userId, messageId, readAt);

            // уведомить всех надо об этом
            wsNotify.notifyMessageReadUpTo(chatId, userId, messageId, readAt);

            log.info("[🔧] ✅ User {} marked message as read {} in chat {}", userId, messageId, chatId);
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

    public ResultOneArg<MessagesPageDTO> getMessagePagination(long chatId, long userId, Long cursor, int limit, Direction direction) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            MessagesPageDTO pagination = dataOrchestrator.getChatMessagesPage(chatId, userId, cursor, limit, direction);

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
    public ResultOneArg<UserMessageDTO> getMessage(long chatId, long userId, long messageId) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            Optional<UserMessageDTO> message = dataOrchestrator.getActiveMessageWithReadStatusInChat(chatId, userId, messageId);
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
    public ResultOneArg<List<MessageReadStatusDTO>> getMessageReads(long chatId, long userId, long messageId) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);
            validator.validateActiveMessageInChat(chatId, messageId);

            List<MessageReadStatusDTO> message = dataOrchestrator.getMessageReads(messageId);

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