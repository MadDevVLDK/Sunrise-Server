package com.sunrise.db.result;

import java.time.LocalDateTime;

public interface UserSecurityResult {
    Long getId();
    String getEmail();
    String getHashPassword();
    Integer getJwtVersion();
    Boolean getIsEnabled();
    LocalDateTime getDeletedAt();
    Boolean getIsDeleted();
}
