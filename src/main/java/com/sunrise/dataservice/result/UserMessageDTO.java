package com.sunrise.dataservice.result;

import com.sunrise.dataservice.type.MessageType;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.AllArgsConstructor
public class UserMessageDTO {
    private long id;
    private long chatId;
    private MessageType messageType;
    private long senderId;
    private LocalDateTime profileUpdatedAt;
    private LocalDateTime memberUpdatedAt;
    private String text;
    private long readCount;
    private boolean readByUser;
    private LocalDateTime sentAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private boolean isDeleted;
}
