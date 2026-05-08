package com.sunrise.db.result;

import java.time.Instant;

public interface MessageReadStatusResult {
    Long getUserId();
    Instant getReadAt();
}
