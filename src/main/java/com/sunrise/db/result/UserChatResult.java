package com.sunrise.db.result;

import java.time.Instant;

public interface UserChatResult {
    Long getId();
    String getName();
    String getDescription();
    String getChatType();
    Integer getMembersCount();
    Instant getUpdatedAt();
    Instant getCreatedAt();
    Long getCreatedBy();
    Instant getDeletedAt();
    Boolean getIsDeleted();
    Long getLastReadMessageId();
    Integer getUnreadCount();
    Long getSeq();


    Long getMsgId();
    Long getMsgChatId();
    Long getMsgSenderId();
    Instant getMsgProfileUpdatedAt();
    Instant getMsgMemberUpdatedAt();
    String getMsgMessageType();
    String getMsgText();
    Long getMsgReadCount();
    Boolean getMsgIsReadByUser();
    Instant getMsgSentAt();
    Instant getMsgUpdatedAt();
    Instant getMsgDeletedAt();
    Boolean getMsgIsDeleted();

    String getSelfMemberTag();
    Instant getSelfMemberSettingsUpdatedAt();
    Instant getSelfMemberUpdatedAt();
    Instant getSelfMemberJoinedAt();
    Boolean getSelfMemberIsPinned();
    Boolean getSelfMemberIsAdmin();

    Long getOpponentId();
    String getOpponentUsername();
    String getOpponentName();
    Instant getOpponentProfileUpdatedAt();
    Instant getOpponentCreatedAt();

    String getOpponentMemberTag();
    Instant getOpponentMemberUpdatedAt();
    Instant getOpponentMemberJoinedAt();
    Boolean getOpponentMemberIsAdmin();

//    Long getAvatarId();
//    String getAvatarHash();
//    String getAvatarPrHash();
//    LocalDateTime getAvatarCreatedAt();
}
