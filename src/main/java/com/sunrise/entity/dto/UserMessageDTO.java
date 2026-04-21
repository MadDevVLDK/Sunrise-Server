package com.sunrise.entity.dto;

import com.sunrise.core.dataservice.type.MessageType;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.AllArgsConstructor
public class UserMessageDTO {
    private long id;
    private long chatId;
    private long senderId;
    private MessageType messageType;
    private LocalDateTime profileUpdatedAt;
    private String text;
    private long readCount;
    private boolean readByUser;
    private LocalDateTime sentAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private boolean isDeleted;
}
