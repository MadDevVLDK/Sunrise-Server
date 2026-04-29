package com.sunrise.helpclass.mapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.sunrise.cache.entity.*;
import com.sunrise.core.creation.*;
import com.sunrise.db.entity.*;
import com.sunrise.db.result.*;
import com.sunrise.orchestrator.result.*;
import com.sunrise.orchestrator.type.MessageType;


public class MessageMapper {


    // ========== MESSAGE ==========

    public static CacheMessage copy(CacheMessage message) {
        if (message == null) return null;

        return new CacheMessage(
            message.getId(),
            message.getChatId(),
            message.getMessageType(),
            message.getSenderId(),
            message.getText(),
            message.getReadCount(),
            message.getSentAt(),
            message.getUpdatedAt(),
            message.getDeletedAt(),
            message.isDeleted()
        );
    }

    public static CacheMessage toCache(CreateMessageDTO message) {
        if (message == null) return null;

        return new CacheMessage(
            message.getId(),
            message.getChatId(),
            message.getMessageType(),
            message.getSenderId(),
            message.getText(),
            message.getReadCount(),
            message.getSentAt(),
            message.getUpdatedAt(),
            message.getDeletedAt(),
            message.isDeleted()
        );
    }

    public static CacheMessage toCache(UserMessageResult message) {
        if (message == null) return null;

        return new CacheMessage(
            message.getId(),
            message.getChatId(),
            MessageType.valueOf(message.getMessageType()),
            message.getSenderId(),
            message.getText(),
            message.getReadCount(),
            message.getSentAt(),
            message.getUpdatedAt(),
            message.getDeletedAt(),
            message.getIsDeleted()
        );
    }

    public static List<CacheMessage> toCaches(List<UserMessageResult> messages) {
        if (messages == null) return null;

        List<CacheMessage> result = new ArrayList<>(messages.size());
        for (UserMessageResult message : messages) {
            result.add(toCache(message));
        }
        return result;
    }

    public static CacheMessage toCache(Message message) {
        if (message == null) return null;

        return new CacheMessage(
            message.getId(),
            message.getChatId(),
            message.getMessageType(),
            message.getSenderId(),
            message.getText(),
            message.getReadCount(),
            message.getSentAt(),
            message.getUpdatedAt(),
            message.getDeletedAt(),
            message.isDeleted()
        );
    }

    public static Message toEntity(CreateMessageDTO message) {
        if (message == null) return null;

        return new Message(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getMessageType(),
            message.getText(),
            message.getReadCount(),
            message.getSentAt(),
            message.getUpdatedAt(),
            message.getDeletedAt(),
            message.isDeleted()
        );
    }

    public static UserMessageDTO toUserDTO(CacheMessage message, Instant profileUpdatedAt, 
                                           Instant memberUpdatedAt, boolean isCensored) {
        if (message == null) return null;

        return new UserMessageDTO(
            message.getId(),
            message.getChatId(),
            message.getMessageType(),
            message.getSenderId(),
            profileUpdatedAt,
            memberUpdatedAt,
            isCensored ? null : message.getText(),
            message.getReadCount(),
            message.getSentAt(),
            message.getUpdatedAt(),
            message.getDeletedAt(),
            message.isDeleted()
        );
    }
    public static UserMessageDTO toUserDTO(UserMessageResult message) {
        if (message == null) return null;

        return new UserMessageDTO(
            message.getId(),
            message.getChatId(),
            MessageType.valueOf(message.getMessageType()),
            message.getSenderId(),
            message.getProfileUpdatedAt(),
            message.getMemberUpdatedAt(),
            message.getText(),
            message.getReadCount(),
            message.getSentAt(),
            message.getUpdatedAt(),
            message.getDeletedAt(),
            message.getIsDeleted()
        );
    }

    public static UserMessageDTO toUserDTO(UserChatResult chat) {
        if (chat == null || chat.getMsgId() == null) return null;

        return new UserMessageDTO(
            chat.getMsgId(),
            chat.getMsgChatId(),
            MessageType.valueOf(chat.getMsgMessageType()),
            chat.getMsgSenderId(),
            chat.getMsgProfileUpdatedAt(),
            chat.getMsgMemberUpdatedAt(),
            chat.getMsgText(),
            chat.getMsgReadCount(),
            chat.getMsgSentAt(),
            chat.getMsgUpdatedAt(),
            chat.getMsgDeletedAt(),
            chat.getMsgIsDeleted()
        );
    }
}
