package com.sunrise.cache.entity;

import com.sunrise.dataservice.type.MessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CacheMessageSecurity {
    private long id;
    private long chatId;
    private long senderId;
    private MessageType messageType;
    private LocalDateTime sentAt;
    private LocalDateTime deletedAt;
    private boolean isDeleted;

    private final LocalDateTime cachedAt = LocalDateTime.now();

    public boolean isActive() {
        return !isDeleted;
    }

    public static CacheMessageSecurity copy(CacheMessageSecurity message) {
        if (message == null) return null;

        return new CacheMessageSecurity(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getMessageType(),
            message.getSentAt(),
            message.getDeletedAt(),
            message.isDeleted()
        );
    }
}