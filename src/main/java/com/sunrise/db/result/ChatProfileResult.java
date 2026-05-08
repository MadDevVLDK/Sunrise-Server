package com.sunrise.db.result;

import java.time.Instant;

public interface ChatProfileResult {
    Long getId();
    String getName();
    String getDescription();
    String getChatType();
    Long getOpponentId();
    Integer getMembersCount();
    Instant getUpdatedAt();
    Instant getCreatedAt();
    Long getCreatedBy();
    Instant getDeletedAt();
    Boolean getIsDeleted();

//    Long getAvatarId();
//    String getAvatarHash();
//    String getAvatarPrHash();
//    Instant getAvatarCreatedAt();
}
