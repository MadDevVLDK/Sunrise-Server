package com.sunrise.entity.dto;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.AllArgsConstructor
public class ChatMemberProfileDTO {
    private long chatId;
    private long userId;
    private String tag;
    private LocalDateTime updatedAt;
    private LocalDateTime joinedAt;
    private boolean isAdmin;
}