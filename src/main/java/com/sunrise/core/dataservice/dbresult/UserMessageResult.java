package com.sunrise.core.dataservice.dbresult;

import java.time.LocalDateTime;

public interface UserMessageResult {
    Long getId();
    Long getChatId();
    Long getSenderId();
    String getMessageType();
    LocalDateTime getProfileUpdatedAt();
    String getText();
    Long getReadCount();
    Boolean getIsReadByUser();
    LocalDateTime getSentAt();
    LocalDateTime getUpdatedAt();
    LocalDateTime getDeletedAt();
    Boolean getIsDeleted();
}