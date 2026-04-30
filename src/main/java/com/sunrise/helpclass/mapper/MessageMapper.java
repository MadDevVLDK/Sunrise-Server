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

    public static Cache.Message copy(Cache.Message message) {
        if (message == null) return null;

        return new Cache.Message(
            message.id(),
            message.chatId(),
            message.messageType(),
            message.senderId(),
            message.text(),
            message.readCount(),
            message.sentAt(),
            message.updatedAt(),
            message.deletedAt(),
            message.isDeleted()
        );
    }

    public static Cache.Message toCache(CreateDto.Message message) {
        if (message == null) return null;

        return new Cache.Message(
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

    public static Cache.Message toCache(UserMessageResult message) {
        if (message == null) return null;

        return new Cache.Message(
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

    public static List<Cache.Message> toCaches(List<UserMessageResult> messages) {
        if (messages == null) return null;

        List<Cache.Message> result = new ArrayList<>(messages.size());
        for (UserMessageResult message : messages) {
            result.add(toCache(message));
        }
        return result;
    }

    public static Cache.Message toCache(Message message) {
        if (message == null) return null;

        return new Cache.Message(
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

    public static Message toEntity(CreateDto.Message message) {
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

    public static Dto.Message toUserDTO(Cache.Message message, Instant profileUpdatedAt, Instant memberUpdatedAt) {
        if (message == null) return null;

        return new Dto.Message(
            message.id(),
            message.chatId(),
            message.messageType(),
            message.senderId(),
            profileUpdatedAt,
            memberUpdatedAt,
            message.text(),
            message.readCount(),
            message.sentAt(),
            message.updatedAt(),
            message.deletedAt(),
            message.isDeleted()
        );
    }
    public static Dto.Message toUserDTO(UserMessageResult message) {
        if (message == null) return null;

        return new Dto.Message(
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

    public static Dto.Message toUserDTO(UserChatResult chat) {
        if (chat == null || chat.getMsgId() == null) return null;

        return new Dto.Message(
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
