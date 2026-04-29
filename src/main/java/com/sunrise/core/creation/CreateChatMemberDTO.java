package com.sunrise.core.creation;

import java.time.Instant;

@lombok.Getter
public class CreateChatMemberDTO {
    private final long chatId;
    private final long userId;
    private final String tag = null;
    private final Instant settingsUpdatedAt;
    private final Instant updatedAt;
    private final Instant joinedAt;
    private final boolean isPinned = false;
    private final boolean isAdmin;
    private final Instant deletedAt = null;
    private final boolean isDeleted = false;

    public CreateChatMemberDTO(long chatId, long userId, Instant joinedAt, boolean isAdmin) {
        this.chatId = chatId;
        this.userId = userId;
        this.settingsUpdatedAt = joinedAt;
        this.updatedAt = joinedAt;
        this.joinedAt = joinedAt;
        this.isAdmin = isAdmin;
    }
}
