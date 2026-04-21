package com.sunrise.core.dataservice.dbresult;

import java.time.LocalDateTime;

public interface UserChatResult {
    Long getId();
    String getName();
    String getDescription();
    String getChatType();
    Long getOpponentId();
    Integer getMembersCount();
    LocalDateTime getUpdatedAt();
    LocalDateTime getCreatedAt();
    Long getCreatedBy();
    LocalDateTime getDeletedAt();
    Boolean getIsDeleted();
    Boolean getIsPinned();
    Integer getUnreadCount();

    Long getMsgId();
    Long getMsgChatId();
    Long getMsgSenderId();
    String getMsgMessageType();
    LocalDateTime getMsgProfileUpdatedAt();
    String getMsgText();
    Long getMsgReadCount();
    Boolean getMsgIsReadByUser();
    LocalDateTime getMsgSentAt();
    LocalDateTime getMsgUpdatedAt();
    LocalDateTime getMsgDeletedAt();
    Boolean getMsgIsDeleted();

    Long getAvatarId();
    String getAvatarHash();
    String getAvatarPrHash();
    LocalDateTime getAvatarCreatedAt();
}
