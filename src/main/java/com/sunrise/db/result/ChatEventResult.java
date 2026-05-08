package com.sunrise.db.result;

import java.time.Instant;

public interface ChatEventResult {
    Long getChatId();
    Long getEventId();
    String getEventType();
    String getPayload();
    Instant getCreatedAt();
}
