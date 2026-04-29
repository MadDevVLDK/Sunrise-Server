package com.sunrise.orchestrator.result;

import java.time.Instant;

@lombok.Getter
@lombok.AllArgsConstructor
public class MessageReadStatusDTO {
    private long userId;
    private Instant readAt;
}
