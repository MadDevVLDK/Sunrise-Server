package com.sunrise.orchestrator.result;

import java.time.Instant;

import com.sunrise.orchestrator.type.MessageType;

@lombok.Getter
@lombok.AllArgsConstructor
public class UserMessageDTO {
    private long id;
    private long chatId;
    private MessageType messageType;
    private long senderId;
    private Instant profileUpdatedAt;
    private Instant memberUpdatedAt;
    private String text;
    private long readCount;
    private Instant sentAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private boolean isDeleted;
}
