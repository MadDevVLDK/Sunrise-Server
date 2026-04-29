package com.sunrise.db.result;

import java.time.Instant;

public interface UserProfileResult {
    Long getId();
    String getUsername();
    String getName();
    Instant getProfileUpdatedAt();
    Instant getCreatedAt();
    Boolean getIsEnabled();
    Instant getDeletedAt();
    Boolean getIsDeleted();

//    Long getAvatarId();
//    String getAvatarHash();
//    String getAvatarPrHash();
//    Instant getAvatarCreatedAt();
}