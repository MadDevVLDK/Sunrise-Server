package com.sunrise.db.result;

import java.time.LocalDateTime;

public interface MessageReadStatusResult {
    Long getUserId();
    LocalDateTime getReadAt();
}
