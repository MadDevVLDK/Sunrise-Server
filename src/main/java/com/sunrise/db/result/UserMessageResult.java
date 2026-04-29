package com.sunrise.db.result;

import java.time.Instant;

public interface UserMessageResult {
    Long getId();
    Long getChatId();
    Long getSenderId();
    String getMessageType();
    Instant getProfileUpdatedAt();
    Instant getMemberUpdatedAt();
    String getText();
    Long getReadCount();
    Instant getSentAt();
    Instant getUpdatedAt();
    Instant getDeletedAt();
    Boolean getIsDeleted();
}