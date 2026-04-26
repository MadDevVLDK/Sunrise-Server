package com.sunrise.db.result;

import java.time.LocalDateTime;

public interface UserProfileResult {
    Long getId();
    String getUsername();
    String getName();
    LocalDateTime getProfileUpdatedAt();
    LocalDateTime getCreatedAt();
    Boolean getIsEnabled();
    LocalDateTime getDeletedAt();
    Boolean getIsDeleted();

//    Long getAvatarId();
//    String getAvatarHash();
//    String getAvatarPrHash();
//    LocalDateTime getAvatarCreatedAt();
}