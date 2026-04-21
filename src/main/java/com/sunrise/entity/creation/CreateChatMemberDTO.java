package com.sunrise.entity.creation;

import java.time.LocalDateTime;

@lombok.Getter
public class CreateChatMemberDTO {
    private final long chatId;
    private final long userId;
    private final String tag = null;
    private final LocalDateTime settingsUpdatedAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime joinedAt;
    private final boolean isPinned = false;
    private final boolean isAdmin;
    private final LocalDateTime deletedAt = null;
    private final boolean isDeleted = false;

    public CreateChatMemberDTO(long chatId, long userId, LocalDateTime joinedAt, boolean isAdmin) {
        this.chatId = chatId;
        this.userId = userId;
        this.settingsUpdatedAt = joinedAt;
        this.updatedAt = joinedAt;
        this.joinedAt = joinedAt;
        this.isAdmin = isAdmin;
    }
}
