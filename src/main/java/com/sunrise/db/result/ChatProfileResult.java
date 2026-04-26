package com.sunrise.db.result;

import java.time.LocalDateTime;

public interface ChatProfileResult {
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

//    Long getAvatarId();
//    String getAvatarHash();
//    String getAvatarPrHash();
//    LocalDateTime getAvatarCreatedAt();
}
