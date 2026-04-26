package com.sunrise.dataservice.result;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.AllArgsConstructor
public class ChatMemberFullDTO {
    private long chatId;
    private long userId;
    private String tag;
    private LocalDateTime settingsUpdatedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime joinedAt;
    private boolean isPinned;
    private boolean isAdmin;
}
