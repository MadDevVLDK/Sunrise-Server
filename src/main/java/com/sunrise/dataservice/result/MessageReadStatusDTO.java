package com.sunrise.dataservice.result;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class MessageReadStatusDTO {
    private long userId;
    private LocalDateTime readAt;
}
