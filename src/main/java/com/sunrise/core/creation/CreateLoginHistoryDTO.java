package com.sunrise.core.creation;

import java.time.Instant;

@lombok.Getter
@lombok.AllArgsConstructor
public class CreateLoginHistoryDTO {
    private long id;
    private long userId;
    private String ipAddress;
    private String deviceInfo;
    private Instant loginAt;
}
