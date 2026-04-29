package com.sunrise.core.creation;

import java.time.Instant;

import com.sunrise.orchestrator.type.MessageType;

@lombok.Getter
public class CreateMessageDTO {
    private final long id;
    private final long chatId;
    private final long senderId;
    private final MessageType messageType;
    private final String text;
    private final long readCount = 0L;
    private final Instant sentAt;
    private final Instant updatedAt;
    private final Instant deletedAt = null;
    private final boolean isDeleted = false;

    public CreateMessageDTO(long id, long chatId, long senderId, MessageType messageType, String text, Instant sentAt) {
        this.id = id;
        this.chatId = chatId;
        this.senderId = senderId;
        this.messageType = messageType;
        this.text = text;
        this.sentAt = sentAt;
        this.updatedAt = sentAt;
    }
}
