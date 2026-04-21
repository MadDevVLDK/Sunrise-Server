package com.sunrise.entity.creation;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.AllArgsConstructor
public class CreateLoginHistoryDTO {
    private long id;
    private long userId;
    private String ipAddress;
    private String deviceInfo;
    private LocalDateTime loginAt;
}
