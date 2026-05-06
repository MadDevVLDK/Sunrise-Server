package com.sunrise.db.result;

import java.time.Instant;

public interface UserEventResult {
    Long getUserId();
    Long getEventId();
    String getEventType();
    String getPayload();
    Instant getCreatedAt();
}
