package com.sunrise.db.result;

import java.time.LocalDateTime;

public interface UserChatResult {
    Long getId();
    String getName();
    String getDescription();
    String getChatType();
    Integer getMembersCount();
    LocalDateTime getUpdatedAt();
    LocalDateTime getCreatedAt();
    Long getCreatedBy();
    LocalDateTime getDeletedAt();
    Boolean getIsDeleted();
    Integer getUnreadCount();

    Long getMsgId();
    Long getMsgChatId();
    Long getMsgSenderId();
    LocalDateTime getMsgProfileUpdatedAt();
    LocalDateTime getMsgMemberUpdatedAt();
    String getMsgMessageType();
    String getMsgText();
    Long getMsgReadCount();
    Boolean getMsgIsReadByUser();
    LocalDateTime getMsgSentAt();
    LocalDateTime getMsgUpdatedAt();
    LocalDateTime getMsgDeletedAt();
    Boolean getMsgIsDeleted();

    String getSelfMemberTag();
    LocalDateTime getSelfMemberSettingsUpdatedAt();
    LocalDateTime getSelfMemberUpdatedAt();
    LocalDateTime getSelfMemberJoinedAt();
    Boolean getSelfMemberIsPinned();
    Boolean getSelfMemberIsAdmin();

    Long getOpponentId();
    String getOpponentUsername();
    String getOpponentName();
    LocalDateTime getOpponentProfileUpdatedAt();
    LocalDateTime getOpponentCreatedAt();

    String getOpponentMemberTag();
    LocalDateTime getOpponentMemberUpdatedAt();
    LocalDateTime getOpponentMemberJoinedAt();
    Boolean getOpponentMemberIsAdmin();

//    Long getAvatarId();
//    String getAvatarHash();
//    String getAvatarPrHash();
//    LocalDateTime getAvatarCreatedAt();
}
