package com.sunrise.entity.creation;

import com.sunrise.core.dataservice.type.MessageType;

import java.time.LocalDateTime;

@lombok.Getter
public class CreateMessageDTO {
    private final long id;
    private final long chatId;
    private final long senderId;
    private final MessageType messageType;
    private final String text;
    private final long readCount = 0L;
    private final LocalDateTime sentAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt = null;
    private final boolean isDeleted = false;

    public CreateMessageDTO(long id, long chatId, long senderId, MessageType messageType, String text, LocalDateTime sentAt) {
        this.id = id;
        this.chatId = chatId;
        this.senderId = senderId;
        this.messageType = messageType;
        this.text = text;
        this.sentAt = sentAt;
        this.updatedAt = sentAt;
    }
}
