package com.sunrise.db.result;

import java.time.Instant;

public interface UserSecurityResult {
    Long getId();
    String getEmail();
    String getHashPassword();
    Integer getJwtVersion();
    Boolean getIsEnabled();
    Instant getDeletedAt();
    Boolean getIsDeleted();
}
